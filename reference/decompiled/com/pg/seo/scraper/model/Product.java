/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnoreProperties
 */
package com.pg.seo.scraper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Product {
    private String title;
    private String url;
    private String image;
    private String price;
    private String soldBy;
    private String fulfilledBy;
    private SellerInfo sellerInfo;

    public String getTitle() {
        return this.title;
    }

    public String getUrl() {
        return this.url;
    }

    public String getImage() {
        return this.image;
    }

    public String getPrice() {
        return this.price;
    }

    public String getSoldBy() {
        return this.soldBy;
    }

    public String getFulfilledBy() {
        return this.fulfilledBy;
    }

    public SellerInfo getSellerInfo() {
        return this.sellerInfo;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setSoldBy(String soldBy) {
        this.soldBy = soldBy;
    }

    public void setFulfilledBy(String fulfilledBy) {
        this.fulfilledBy = fulfilledBy;
    }

    public void setSellerInfo(SellerInfo sellerInfo) {
        this.sellerInfo = sellerInfo;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Product)) {
            return false;
        }
        Product other = (Product)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$url = this.getUrl();
        String other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) {
            return false;
        }
        String this$image = this.getImage();
        String other$image = other.getImage();
        if (this$image == null ? other$image != null : !this$image.equals(other$image)) {
            return false;
        }
        String this$price = this.getPrice();
        String other$price = other.getPrice();
        if (this$price == null ? other$price != null : !this$price.equals(other$price)) {
            return false;
        }
        String this$soldBy = this.getSoldBy();
        String other$soldBy = other.getSoldBy();
        if (this$soldBy == null ? other$soldBy != null : !this$soldBy.equals(other$soldBy)) {
            return false;
        }
        String this$fulfilledBy = this.getFulfilledBy();
        String other$fulfilledBy = other.getFulfilledBy();
        if (this$fulfilledBy == null ? other$fulfilledBy != null : !this$fulfilledBy.equals(other$fulfilledBy)) {
            return false;
        }
        SellerInfo this$sellerInfo = this.getSellerInfo();
        SellerInfo other$sellerInfo = other.getSellerInfo();
        return !(this$sellerInfo == null ? other$sellerInfo != null : !((Object)this$sellerInfo).equals(other$sellerInfo));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Product;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        String $image = this.getImage();
        result = result * 59 + ($image == null ? 43 : $image.hashCode());
        String $price = this.getPrice();
        result = result * 59 + ($price == null ? 43 : $price.hashCode());
        String $soldBy = this.getSoldBy();
        result = result * 59 + ($soldBy == null ? 43 : $soldBy.hashCode());
        String $fulfilledBy = this.getFulfilledBy();
        result = result * 59 + ($fulfilledBy == null ? 43 : $fulfilledBy.hashCode());
        SellerInfo $sellerInfo = this.getSellerInfo();
        result = result * 59 + ($sellerInfo == null ? 43 : ((Object)$sellerInfo).hashCode());
        return result;
    }

    public String toString() {
        return "Product(title=" + this.getTitle() + ", url=" + this.getUrl() + ", image=" + this.getImage() + ", price=" + this.getPrice() + ", soldBy=" + this.getSoldBy() + ", fulfilledBy=" + this.getFulfilledBy() + ", sellerInfo=" + this.getSellerInfo() + ")";
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class SellerInfo {
        private String sellerName;
        private String sellerRating;
        private String totalRatings;
        private String businessName;
        private String businessAddress;

        public String getSellerName() {
            return this.sellerName;
        }

        public String getSellerRating() {
            return this.sellerRating;
        }

        public String getTotalRatings() {
            return this.totalRatings;
        }

        public String getBusinessName() {
            return this.businessName;
        }

        public String getBusinessAddress() {
            return this.businessAddress;
        }

        public void setSellerName(String sellerName) {
            this.sellerName = sellerName;
        }

        public void setSellerRating(String sellerRating) {
            this.sellerRating = sellerRating;
        }

        public void setTotalRatings(String totalRatings) {
            this.totalRatings = totalRatings;
        }

        public void setBusinessName(String businessName) {
            this.businessName = businessName;
        }

        public void setBusinessAddress(String businessAddress) {
            this.businessAddress = businessAddress;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof SellerInfo)) {
                return false;
            }
            SellerInfo other = (SellerInfo)o;
            if (!other.canEqual(this)) {
                return false;
            }
            String this$sellerName = this.getSellerName();
            String other$sellerName = other.getSellerName();
            if (this$sellerName == null ? other$sellerName != null : !this$sellerName.equals(other$sellerName)) {
                return false;
            }
            String this$sellerRating = this.getSellerRating();
            String other$sellerRating = other.getSellerRating();
            if (this$sellerRating == null ? other$sellerRating != null : !this$sellerRating.equals(other$sellerRating)) {
                return false;
            }
            String this$totalRatings = this.getTotalRatings();
            String other$totalRatings = other.getTotalRatings();
            if (this$totalRatings == null ? other$totalRatings != null : !this$totalRatings.equals(other$totalRatings)) {
                return false;
            }
            String this$businessName = this.getBusinessName();
            String other$businessName = other.getBusinessName();
            if (this$businessName == null ? other$businessName != null : !this$businessName.equals(other$businessName)) {
                return false;
            }
            String this$businessAddress = this.getBusinessAddress();
            String other$businessAddress = other.getBusinessAddress();
            return !(this$businessAddress == null ? other$businessAddress != null : !this$businessAddress.equals(other$businessAddress));
        }

        protected boolean canEqual(Object other) {
            return other instanceof SellerInfo;
        }

        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            String $sellerName = this.getSellerName();
            result = result * 59 + ($sellerName == null ? 43 : $sellerName.hashCode());
            String $sellerRating = this.getSellerRating();
            result = result * 59 + ($sellerRating == null ? 43 : $sellerRating.hashCode());
            String $totalRatings = this.getTotalRatings();
            result = result * 59 + ($totalRatings == null ? 43 : $totalRatings.hashCode());
            String $businessName = this.getBusinessName();
            result = result * 59 + ($businessName == null ? 43 : $businessName.hashCode());
            String $businessAddress = this.getBusinessAddress();
            result = result * 59 + ($businessAddress == null ? 43 : $businessAddress.hashCode());
            return result;
        }

        public String toString() {
            return "Product.SellerInfo(sellerName=" + this.getSellerName() + ", sellerRating=" + this.getSellerRating() + ", totalRatings=" + this.getTotalRatings() + ", businessName=" + this.getBusinessName() + ", businessAddress=" + this.getBusinessAddress() + ")";
        }
    }
}

