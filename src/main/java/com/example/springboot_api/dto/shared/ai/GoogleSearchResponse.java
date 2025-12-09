package com.example.springboot_api.dto.shared.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleSearchResponse {

    private SearchInformation searchInformation;
    private List<GoogleSearchItem> items;

    public SearchInformation getSearchInformation() {
        return searchInformation;
    }

    public void setSearchInformation(SearchInformation searchInformation) {
        this.searchInformation = searchInformation;
    }

    public List<GoogleSearchItem> getItems() {
        return items;
    }

    public void setItems(List<GoogleSearchItem> items) {
        this.items = items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchInformation {
        private Double searchTime;

        public Double getSearchTime() {
            return searchTime;
        }

        public void setSearchTime(Double searchTime) {
            this.searchTime = searchTime;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleSearchItem {
        private String title;
        private String link;
        private String snippet;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getSnippet() {
            return snippet;
        }

        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }
    }
}

