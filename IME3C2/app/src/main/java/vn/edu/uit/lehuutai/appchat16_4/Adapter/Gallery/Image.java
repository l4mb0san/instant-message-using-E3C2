package vn.edu.uit.lehuutai.appchat16_4.Adapter.Gallery;

/**
 * Created by lehuu on 5/12/2017.
 */

public class Image {

    String imagePath;
    String imageName;

    public Image(String imageName, String imagePath) {
        this.imagePath = imagePath;
        this.imageName = imageName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
