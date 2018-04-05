package thepseudoartist.ecell_json;

public class NewsArticle {
    public String NewsURL;
    public String imageUrl;
    public String NewsTitle;
    public String NewsDescription;
    public String NewsTime;

    public NewsArticle(String url, String title, String desc, String image, String time) {
        NewsURL = url;
        NewsTitle = title;
        NewsDescription = desc;
        imageUrl = image;
        NewsTime = time;

    }
}
