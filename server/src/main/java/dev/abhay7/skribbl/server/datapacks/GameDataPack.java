package dev.abhay7.skribbl.server.datapacks;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

public class GameDataPack extends DataPackage {

    private String message = "";
    private BufferedImage image;

    public GameDataPack(String message) {
        this(true, message);
    }

    public GameDataPack(BufferedImage image) {
        this(true, image);
    }

    public GameDataPack(boolean isServerRequest, String message) {
        super(isServerRequest);
        this.message = message;
    }

    public GameDataPack(boolean isServerRequest, BufferedImage image) {
        super(isServerRequest);
        this.image = image;
    }

    public static GameDataPack fromJSON(String json) throws JSONException {
        return fromJSON(new JSONObject(json));
    }
    public static GameDataPack fromJSON(JSONObject jo) throws JSONException {
        boolean isServerRequest = jo.getBoolean("isServerRequest");
        
        if(jo.has("image")) {
            try {
                byte[] bytes = Base64.getDecoder().decode(jo.getString("image"));
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                return new GameDataPack(isServerRequest, img);
            } catch(IOException ioe) {
                System.out.println(ioe);
                return null;
            }
        } else if(jo.has("message") && !jo.getString("message").isBlank()) {
            return new GameDataPack(isServerRequest, jo.getString("message"));
        }

        return null;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());

        if(this.message != null) {
            jo.put("message", this.getMessage());
        }

        if(this.image != null) {

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(this.image, "png", baos);
                jo.put("image", Base64.getEncoder().encodeToString(baos.toByteArray()));
            } catch(IOException ioe) {
                return jo;
            }
        }

        return jo;
    }

    public String getMessage() { return this.message; }
    public BufferedImage getImage() { return this.image; }
    
}
