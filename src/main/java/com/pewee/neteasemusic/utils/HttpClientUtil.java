package com.pewee.neteasemusic.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.web.multipart.MultipartFile;

import com.pewee.neteasemusic.exceptions.ComponentLoadException;
import com.pewee.neteasemusic.models.common.AttachmentInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * @author by GongRan
 * @Classname HttpClientUtil
 * @Description httpClient的工具类, 有如下功能:
 * 1.初始化HttpClient,设置池策略(不支持重试,暂时不支持cookie) --- 完成
 * 2.支持get操作 --- 完成
 * 3.支持post操作(表单) --- 完成
 * 4.支持postJson操作 --- 完成
 * 4.支持post提交附件 --- 完成
 * @Date 2022/7/21 17:01
 */
@Slf4j
public class HttpClientUtil {
    private static final RequestConfig rc;

    private static CloseableHttpClient httpClient;

    private static PoolingHttpClientConnectionManager cm;


    static {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        //禁止重试
        httpClientBuilder.disableAutomaticRetries();
        //绕过不安全https认证
        Registry<ConnectionSocketFactory> registry;
        try {
            registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", trustHttpsCertificates())
                    .build();
        } catch (Exception e) {
            log.error("HttpClientUtil组件失败... :" + e.getMessage());
            throw new ComponentLoadException("HttpClientUtil组件加载失败", e);
        }

        cm = new PoolingHttpClientConnectionManager(registry);
        //客户端总并行链接最大数
        cm.setMaxTotal(500);
        //每个主机的最大并行链接数
        cm.setDefaultMaxPerRoute(50);

        httpClientBuilder.setConnectionManager(cm);

        rc = RequestConfig.custom()
                //建立连接的超时时间
                .setConnectTimeout(100000)
                //连接建立后,到拿到响应的超时时间
                .setSocketTimeout(100000)
                //从池子中获取连接的超时时间
                .setConnectionRequestTimeout(50000)
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        httpClientBuilder.setDefaultRequestConfig(rc);

        //设置一些默认的头(装作浏览器)
        List<Header> defaultHeaders = new ArrayList<>();
        BasicHeader userAgentHeader = new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
        defaultHeaders.add(userAgentHeader);
        httpClientBuilder.setDefaultHeaders(defaultHeaders);
        httpClient = httpClientBuilder.build();
    }

    public static CloseableHttpClient getInstance() {
        return httpClient;
    }

    public static String executeGet(String url, Map<String, String> headers, Map<String, String> paraMaps) throws IOException, URISyntaxException {
        return executeGet(url,headers,paraMaps,Consts.UTF_8);
    }

    public static String executeGet(String url, Map<String, String> headers, Map<String, String> paraMaps, Charset charset) throws IOException, URISyntaxException {
        log.info("准备执行get,url:{},headers:{},paraMaps:{}", url, headers, paraMaps);
        HttpGet get = null;
        try {
            get = encapsulateHttpGet(url, headers, paraMaps);
        } catch (URISyntaxException e) {
            throw e;
        }
        CloseableHttpResponse response = null;
        String result = null;
        try {
            response = httpClient.execute(get);
            HttpEntity entity = (HttpEntity) response.getEntity();
            result = EntityUtils.toString((org.apache.http.HttpEntity) entity, charset);
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw e;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("执行executeGet,关闭response异常...", e);
                }
            }
        }
        log.info("httpclient调用返回结果: {}",result);
        return result;
    }

    public static AttachmentInfo getAttachment(String url, Map<String, String> headers, Map<String, String> paraMaps) throws IOException, URISyntaxException {
        log.info("准备执行getAttachment,url:{},headers:{},paraMaps:{}", url, headers, paraMaps);
        HttpGet get = null;
        try {
            get = encapsulateHttpGet(url, headers, paraMaps);
        } catch (URISyntaxException e) {
            throw e;
        }

        CloseableHttpResponse response = null;
        HttpEntity httpEntity = null;
        try {
            response = httpClient.execute(get);
            httpEntity = response.getEntity();
            boolean isJsonMimeType = httpEntity.getContentType().getValue().contains("application/json");
            if (isJsonMimeType) {
                return new AttachmentInfo().setSuccess(false).setErrorJson(EntityUtils.toString(httpEntity));
            }
            InputStream inputStream = httpEntity.getContent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer,0,len);
            }
            AttachmentInfo result = new AttachmentInfo().setSuccess(true).setData(baos.toByteArray());
            baos.close();
            EntityUtils.consume(httpEntity);
            return result;
        } catch (IOException e) {
            throw e;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("执行getAttachment,关闭response异常...", e);
                }
            }
        }
    }

    public static String postForm(String url, Map<String, String> headers, Map<String, String> formMap) throws IOException {
        log.info("准备执行postForm,url:{},headers:{},fromMap:{}", url, headers, formMap);
        HttpPost post = new HttpPost(url);
        post.setConfig(rc);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                post.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        //设置form
        List<NameValuePair> list = new ArrayList<>();
        if (formMap != null) {
            for (Map.Entry<String, String> entry : formMap.entrySet()) {
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        CloseableHttpResponse response = null;
        String result = null;
        try {
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(list, Consts.UTF_8);
            post.setEntity(formEntity);
            response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, Consts.UTF_8);
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw e;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("执行postForm,关闭response异常...", e);
                }
            }
        }
        log.info("httpclient调用返回结果: {}",result);
        return result;
    }
    
    public static Pair<String,Header[]> postFormAndReturnHeaders(String url, Map<String, String> headers, Map<String, String> formMap) throws IOException {
        log.info("准备执行postForm,url:{},headers:{},fromMap:{}", url, headers, formMap);
        HttpPost post = new HttpPost(url);
        post.setConfig(rc);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                post.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        //设置form
        List<NameValuePair> list = new ArrayList<>();
        if (formMap != null) {
            for (Map.Entry<String, String> entry : formMap.entrySet()) {
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        CloseableHttpResponse response = null;
        String result = null;
        Header[] allHeaders= null;
        try {
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(list, Consts.UTF_8);
            post.setEntity(formEntity);
            response = httpClient.execute(post);
            allHeaders = response.getAllHeaders();
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, Consts.UTF_8);
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw e;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("执行postForm,关闭response异常...", e);
                }
            }
        }
        log.info("httpclient调用返回结果: {}",result);
        return  Pair.of(result, allHeaders);
    }

    public static String postJson(String url, Map<String, String> headers, String json) throws IOException {
        log.info("准备执行postJson,url:{},headers:{},json:{}", url, headers, json);
        HttpPost post = new HttpPost(url);
        post.setConfig(rc);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!"Content-Type".equalsIgnoreCase(entry.getKey())) {
                    post.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
                }
            }
        }
        CloseableHttpResponse response = null;
        String result = null;
        try {
            StringEntity jsonEntity = new StringEntity(json, Consts.UTF_8);
            jsonEntity.setContentType("application/json;charset=utf-8");
            post.setEntity(jsonEntity);
            response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, Consts.UTF_8);
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw e;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("执行postJson,关闭response异常...", e);
                }
            }
        }
        log.info("httpclient调用返回结果: {}",result);
        return result;
    }

    public static String postWithAttachment(String url, Map<String, String> headers, Map<String, String> formMap, List<MultipartFile> multipartFiles) throws IOException {
        log.info("准备执行postWithAttachment,url:{},headers:{},formMap:{}", url, headers, formMap);
        HttpPost post = new HttpPost(url);
        post.setConfig(rc);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!"Content-Type".equalsIgnoreCase(entry.getKey())) {
                    post.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
                }
            }
        }
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.setCharset(Consts.UTF_8);
        multipartEntityBuilder.setContentType(ContentType.create("multipart/form-data", Consts.UTF_8));
        //设置浏览器模式
        multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        if (multipartFiles != null) {
            for (MultipartFile multipartFile : multipartFiles) {
                //添加附件
                multipartEntityBuilder.addBinaryBody("fileName", multipartFile.getInputStream(), ContentType.create(multipartFile.getContentType()), multipartFile.getOriginalFilename());
            }
        }

        if (formMap != null) {
            for (Map.Entry<String, String> entry : formMap.entrySet()) {
                //添加表单key-value,要解决中文乱码问题
                StringBody stringBody = new StringBody(entry.getValue(), ContentType.create("text/plain", Consts.UTF_8));
                multipartEntityBuilder.addPart(entry.getKey(), stringBody);
            }
        }

        CloseableHttpResponse response = null;
        String result = null;

        try {
            HttpEntity multipartEntity = multipartEntityBuilder.build();
            post.setEntity(multipartEntity);
            response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, Consts.UTF_8);
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            throw e;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("执行postWithAttachment,关闭response异常...", e);
                }
            }
        }
        log.info("httpclient调用返回结果: {}",result);
        return result;
    }

    private static ConnectionSocketFactory trustHttpsCertificates() throws Exception {
    	SSLContext sslContext = SSLContextBuilder.create()
    		    .loadTrustMaterial(new TrustAllStrategy()) // 信任所有证书 (不安全!)
    		    .build();
        return new SSLConnectionSocketFactory(sslContext,NoopHostnameVerifier.INSTANCE);
    }

    private static HttpGet encapsulateHttpGet(String url, Map<String, String> headers, Map<String, String> paraMaps) throws URISyntaxException {
        URIBuilder uriBuilder;
        uriBuilder = new URIBuilder(url);

        if (paraMaps != null) {
            List<NameValuePair> paramList = new LinkedList<>();
            for (Map.Entry<String, String> entry : paraMaps.entrySet()) {
                paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            uriBuilder.setParameters(paramList);
        }
        HttpGet get = null;
        get = new HttpGet(uriBuilder.build());
        get.setConfig(rc);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                get.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        return get;
    }
    
    public static InputStream getInputStream(String url,Map<String,String> headers){
		if(null == headers) {
			headers = new HashMap<>();
		}
		HttpGet get = new HttpGet(url);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				get.addHeader(k, v);
			} );
		}
		get.setConfig(rc);
		try {
			CloseableHttpResponse response = httpClient.execute(get);
			HttpEntity entity = response.getEntity();
			InputStream content = entity.getContent();
			return content;
		}	catch (ClientProtocolException e) {
			log.error(e.getMessage(),e);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		} 
		return null;
	}
    
}
