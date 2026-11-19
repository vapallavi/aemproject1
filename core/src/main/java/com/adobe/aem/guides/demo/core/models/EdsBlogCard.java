package com.adobe.aem.guides.demo.core.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EdsBlogCard {
    private String title;
    private String summary;
    private String imageAlt;
    private String heroImage;
    private String link;
    private List<String> tags;
    private List<String> categories;
    private String author;
    private String readDuration;
    private Date firstPublishedDate;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getImageAlt() { return imageAlt; }
    public void setImageAlt(String imageAlt) { this.imageAlt = imageAlt; }

    public String getHeroImage() { return heroImage; }
    public void setHeroImage(String heroImage) { this.heroImage = heroImage; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getReadDuration() { return readDuration; }
    public void setReadDuration(String readDuration) { this.readDuration = readDuration; }

    public Date getFirstPublishedDate() { return firstPublishedDate; }
    public void setFirstPublishedDate(Date firstPublishedDate) { this.firstPublishedDate = firstPublishedDate; }

    /** Used by tests */
    public void setPublishedDate(Date date) {
        this.firstPublishedDate = date;
    }

    /** Formats date as "MMMM d, yyyy" (e.g. February 6, 2023) */
    public String getFormattedDate() {
        if (firstPublishedDate == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        return sdf.format(firstPublishedDate);
    }
}