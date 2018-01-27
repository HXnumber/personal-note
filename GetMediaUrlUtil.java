package cn.com.datalk.cloud.weixin.util;

import cn.com.datalk.cloud.weixin.aes.AesException;
import cn.com.datalk.cloud.weixin.constant.WeixinMsgSysConst;
import com.google.gson.Gson;
import net.sf.json.JSONObject;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by hx on 2018/1/26.
 * 上传图片素材/上传图文消息内的图片获取URL
 */
public class GetMediaUrlUtil {
    private static Logger logger = LoggerFactory.getLogger(GetMediaUrlUtil.class);

    //替换字符串中的src
    //ynr 图文内容
    //index 从第几个字符开始替换
    public static String thsrc(String ynr,int index,String accessToken){

        int srcStart = ynr.indexOf("src=\"",index); //获取src出现的位置
        if(srcStart==-1){
            return ynr;
        }
        int srcEnd = ynr.indexOf("\"",srcStart+7);
        srcStart = srcStart + 5;
        String src = ynr.substring(srcStart,srcEnd); //获取图片路径

        //执行上传图片方法
        String url = WeixinMsgSysConst.getWeixinUploadimgUrl(accessToken);
        String scptjg = postFile(url,src);
        JSONObject scptjgJson = JSONObject.fromObject(scptjg);
        String newPath = scptjgJson.getString("url");
        //替换字符串中该图片路径
        ynr = ynr.replace(src, newPath);
        //查看字符串下方是否还有img标签
        int sfhyImg = ynr.indexOf("<img",srcEnd);
        if(sfhyImg==-1){
            return ynr;
        }else{
            return thsrc(ynr, srcEnd,accessToken);
        }
    }

    //上传图片素材/上传图文消息内的图片获取URL
    // url - 路径
    //filePath 图片绝对路径
    public static String postFile(String url, String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            return null;
        String result = "";
        try {
            URL url1 = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Cache-Control", "no-cache");
            String boundary = "-----------------------------" + System.currentTimeMillis();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream output = conn.getOutputStream();
            output.write(("--" + boundary + "\r\n").getBytes());
            output.write(
                    String.format("Content-Disposition: form-data; name=\"media\"; filename=\"%s\"\r\n", file.getName())
                            .getBytes());
            output.write("Content-Type: image/jpeg \r\n\r\n".getBytes());
            byte[] data = new byte[1024];
            int len = 0;
            FileInputStream input = new FileInputStream(file);
            while ((len = input.read(data)) > -1) {
                output.write(data, 0, len);
            }
            output.write(("\r\n--" + boundary + "\r\n\r\n").getBytes());
            output.flush();
            output.close();
            input.close();
            InputStream resp = conn.getInputStream();
            StringBuffer sb = new StringBuffer();
            while ((len = resp.read(data)) > -1)
                sb.append(new String(data, 0, len, "utf-8"));
            resp.close();
            result = sb.toString();
        } catch (ClientProtocolException e) {
            logger.error("postFile，不支持http协议", e);
        } catch (IOException e) {
            logger.error("postFile数据传输失败", e);
        }
        return result;
    }

    /**
     * 上传图文消息内的图片获取URL
     * @param rootpath 应用根目录
     * @param systemService systemService
     * @param appid 公众号应用id
     * @param filepath 文件路径
     * @return 图片url
     * @throws IOException
     * @throws AesException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static String getMediaUrl(String accessToken,String filepath) throws IOException, AesException, NoSuchAlgorithmException, KeyManagementException {
        String mediaUrl = GetMediaUrlUtil.postFile(accessToken,filepath);
        Map result = new Gson().fromJson(mediaUrl,Map.class);
        return (String) result.get("url");
    }

    public static String getThumbMediaId(String accessToken,String filepath,String type) throws IOException, AesException, NoSuchAlgorithmException, KeyManagementException {
        String mediaUrl = GetMediaUrlUtil.postFile(WeixinMsgSysConst.getWeixinThumbMediaIdUrl(accessToken,type),filepath);
        Map result = new Gson().fromJson(mediaUrl,Map.class);
        return (String) result.get("media_id");
    }

    public static String uploadNews(String news,String accessToken) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        return HttpClientUtil.doHttpsPost(WeixinMsgSysConst.getWeixinUplodenewsUrl(accessToken),news);
    }

    public static String getUploadedNewsMediaId(String returnBack){
        Map result = new Gson().fromJson(returnBack,Map.class);
        return (String) result.get("media_id");
    }

    public static String getAddMaterialMediaId(String accessToken,String filepath,String type) throws IOException, AesException, NoSuchAlgorithmException, KeyManagementException {
        String mediaUrl = GetMediaUrlUtil.postFile(WeixinMsgSysConst.getWeixinAddMaterialUrl(accessToken,type),filepath);
        Map result = new Gson().fromJson(mediaUrl,Map.class);
        return (String) result.get("media_id");
    }
}
