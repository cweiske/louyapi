package de.cweiske.ouya.louyapi;

import static de.cweiske.ouya.louyapi.HttpService.TAG;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {

    private final AssetManager assetManager;
    protected String prefix = "stouyapi-www";

    public HttpServer(int port, AssetManager assetManager) {
        super(port);
        this.assetManager = assetManager;
    }

    /**
     * This is basically a re-implementation of the stouyapi .htaccess file
     *
     * @param session The HTTP session
     * @return HTTP response
     */
    public Response serve(IHTTPSession session) {
        String path = session.getUri();
        Log.d(TAG, "serve: " + path);
        //this happens with "//agreements/marketplace.html". remove double slash.
        if (path.startsWith("//")) {
            path = path.substring(1);
        }

        if (session.getMethod() == Method.POST || session.getMethod() == Method.PUT) {
            Map<String, String> parameters = new HashMap<String, String>();
            try {
                session.parseBody(parameters);
            } catch (Exception e) {
                //we do not care about the content
                //we only parse the body to prevent errors, see
                // https://github.com/NanoHttpd/nanohttpd/issues/356
            }
        }

        InputStream content;

        if (path.equals("/api/v1/status") || path.equals("/generate_204")) {
            //usage: check if internet connection is working
            return newFixedLengthResponse(Response.Status.NO_CONTENT, null, "");

        } else if (path.equals("/api/v1/details") && session.getParameters().containsKey("app")) {
            //usage: detail page for installed games
            if (null != (content = loadFileContent("/api/v1/details-data/" + session.getParameters().get("app").get(0) + ".json"))) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            } else {
                content = loadFileContent("/api/v1/details-dummy/error-unknown-app-details.json");
                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            }

        } else if (path.startsWith("/api/v1/apps/") && path.endsWith("/download")) {
            String appid = path.substring(13, path.length() - 9);
            if (null != (content = loadFileContent("/api/v1/apps/" + appid + "-download.json"))) {

                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            }

        } else if (path.startsWith("/api/v1/apps/")) {
            String appid = path.substring(13);
            if (null != (content = loadFileContent("/api/v1/apps/" + appid + ".json"))) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            }

        } else if (path.startsWith("/api/v1/developers") && path.endsWith("/products/") && session.getParameters().containsKey("only")) {
            //usage: product details for a single product
            if (null != (content = loadFileContent(path + session.getParameters().get("only").get(0) + ".json"))) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            }

        } else if (path.equals("/api/v1/discover")) {
            //usage: main store page
            if (null != (content = loadFileContent("/api/v1/discover-data/discover.json"))) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            }

        } else if (path.startsWith("/api/v1/discover/")) {
            //usage: store category
            if (null != (content = loadFileContent("/api/v1/discover-data/" + path.substring(17) + ".json"))) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            }

        } else if (path.equals("/api/v1/gamers")) {
            //usage: register a user
            if (null != (content = loadFileContent("/api/v1/gamers/register-error.json"))) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", content);
            }

        } else if (path.equals("/api/v1/gamers/me")) {
            //usage: fetch user data
            if (null != (content = loadFileContent("/api/v1/gamers/me.json"))) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            }

        } else if (path.equals("/api/v1/gamers/key")) {
            //usage: store gamer ouya public key via PUT
            return newFixedLengthResponse(Response.Status.CREATED, null, "");

        } else if (path.equals("/api/v1/search") && session.getParameters().containsKey("q")) {
            //usage: search for games
            String query = session.getParameters().get("q").get(0);
            if (null != (content = loadFileContent("/api/v1/search-data/" + query.charAt(0) + ".json"))) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            }

        } else if (!path.endsWith("/") && null != (content = loadFileContent(path))) {
            //try if the path exists in the assets/stouyapi-www/ dir
            return newFixedLengthResponse(Response.Status.OK, mimeTypeFromPath(path), content);

        } else if (null != (content = loadFileContent(path.replaceAll("/$", "") + "/index.htm"))) {
            //usage: /api/v1/developers/*/products
            return newFixedLengthResponse(Response.Status.OK, "text/html", content);

        } else if (!path.endsWith("/") && null != (content = loadAssetsFileContent(path))) {
            //try if the path exists in the assets/ dir (without stouyapi-www)
            return newFixedLengthResponse(Response.Status.OK, mimeTypeFromPath(path), content);

        } else if (null != (content = loadAssetsFileContent(path.replaceAll("/$", "") + "/index.htm"))) {
            //try if the path + index.htm exists in the assets/ dir (without stouyapi-www)
            return newFixedLengthResponse(Response.Status.OK, "text/html", content);

        } else if (path.startsWith("/api/v1/games/") && path.endsWith("/purchases")) {
            //usage: purchases for non-existing game
            if (null != (content = loadFileContent("/api/v1/games/purchases-empty.json"))) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", content);
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found");
    }

    /**
     * Automatically determine the length of the input stream
     */
    protected Response newFixedLengthResponse(Response.IStatus status, String mimeType, InputStream data) {
        int length = 0;
        try {
            length = streamLength(data);
            data.reset();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "text/plain",
                "Error: " + e.getMessage()
            );
        }
        return newFixedLengthResponse(status, mimeType, data, length);
    }

    protected InputStream loadFileContent(String path)
    {
        return loadAssetsFileContent(prefix + path);
    }

    protected InputStream loadAssetsFileContent(String path)
    {
        path = path.replaceAll("^/", "");
        try {
            Log.d(TAG, "loadFileContent: " + path);
            return assetManager.open(path, AssetManager.ACCESS_BUFFER);
        } catch (IOException e) {
            //file does not exist
            Log.d(TAG, "loadFileContent: fail");
            return null;
        }
    }

    protected static int streamLength(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int chunkBytesRead = 0;
        int length = 0;
        while((chunkBytesRead = inputStream.read(buffer)) != -1) {
            length += chunkBytesRead;
        }
        return length;
    }

    protected String mimeTypeFromPath(String path) {
        if (path.endsWith(".htm")) {
            return "text/html";
        } else if (path.endsWith(".ico")) {
            return "image/vnd.microsoft.icon";
        } else if (path.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (path.endsWith(".png")) {
            return "image/png";
        }
        return "text/plain";
    }

    protected boolean isBinary(String path) {
        return path.endsWith(".ico")
            || path.endsWith(".jpg")
            || path.endsWith(".png");
    }
}
