package thepseudoartist.ecell_json;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Request;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;


public class MainActivity extends AppCompatActivity {

    String News_URL = "https://newsapi.org/v2/top-headlines?country=in&apiKey=3ca16d61807440f594a31fe03cf45054";

    ArrayList<NewsArticle> NewsArticles = new ArrayList<>();
    private RecyclerView recyclerView;
    private RelativeLayout layout;
    private RecyclerView.Adapter adapter;
    private SnapHelper helper;

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isNetworkAvailable()) {
            layout = findViewById(R.id.altLayout);
            layout.setVisibility(View.GONE);

            recyclerView = findViewById(R.id.recycler_view);
            helper = new PagerSnapHelper();
            helper.attachToRecyclerView(recyclerView);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setHasFixedSize(true);

            requestQueue = Volley.newRequestQueue(this);

            parseJSON();

            Log.d("size", String.valueOf(NewsArticles.size()));

        } else {
            Toast.makeText(getApplicationContext(), "No Internet Connection found.", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public class NewsRecyclerView extends RecyclerView.Adapter<NewsRecyclerAdapter> {

        ArrayList<NewsArticle> Articles;

        public NewsRecyclerView(ArrayList<NewsArticle> list) {
            Articles = list;
        }

        @NonNull
        @Override
        public NewsRecyclerAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getApplicationContext())
                    .inflate(R.layout.cardview_layout, parent, false);

            NewsRecyclerAdapter adapter = new NewsRecyclerAdapter(view);
            return adapter;
        }

        @Override
        public void onBindViewHolder(@NonNull final NewsRecyclerAdapter holder, final int position) {

            holder.titleText.setText(Articles.get(position).NewsTitle);

            if(Articles.get(position).NewsDescription == "null")
                holder.descText.setText("No Description Available..");
            else
                holder.descText.setText(Articles.get(position).NewsDescription + "...");

            holder.timeText.setText(Articles.get(position).NewsTime.replace("T"," "));

            try {
                Picasso.get()
                        .load(Articles.get(position).imageUrl)
                        .fit()
                        .error(R.drawable.newspaper)
                        .into(holder.imageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                holder.progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Exception e) {
                                holder.progressBar.setVisibility(View.GONE);
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
            holder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Articles.get(position).NewsURL != "NA") {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Articles.get(position).NewsURL));
                        startActivity(browserIntent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return Articles.size();
        }
    }

    public class NewsRecyclerAdapter extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleText;
        TextView descText;
        TextView timeText;
        RelativeLayout layout;
        ProgressBar progressBar;

        public NewsRecyclerAdapter(View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.card_image);
            titleText = itemView.findViewById(R.id.card_title);
            descText = itemView.findViewById(R.id.card_details);
            timeText = itemView.findViewById(R.id.card_time);
            progressBar = itemView.findViewById(R.id.progressBar);
            layout = itemView.findViewById(R.id.main_card);
        }
    }

    private void parseJSON() {
        adapter = new NewsRecyclerView(NewsArticles);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(com.android.volley.Request.Method.GET, News_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("articles");
                    NewsArticle currentArticle;

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject article = jsonArray.getJSONObject(i);
                        String newsURL = article.getString("url");
                        String imageURL = article.getString("urlToImage");
                        String title = article.getString("title");
                        String description = article.getString("description");
                        String time = article.getString("publishedAt");

                        currentArticle = new NewsArticle(newsURL, title, description, imageURL, time);
                        NewsArticles.add(currentArticle);

                        adapter.notifyDataSetChanged();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        NewsArticles.add(new NewsArticle("NA", "Welcome to News.", "Programmed By: \n\nRishabh Vishwakarma \nElectrical Engineering \nII Semester", "NA", "For: E-Cell"));

        requestQueue.add(jsonObjectRequest);

    }
}


