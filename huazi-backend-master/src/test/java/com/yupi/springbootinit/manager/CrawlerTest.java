package com.yupi.springbootinit.manager;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.springbootinit.model.entity.Picture;
import com.yupi.springbootinit.model.entity.Post;
import com.yupi.springbootinit.service.PostService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class CrawlerTest {
@Resource
private PostService postService;

   @Test
   void testFetchPicture() throws IOException {
       int current=1;
       String url="https://cn.bing.com/images/search?q=小黑子&first=" + current;
       Document doc = Jsoup.connect(url).get();
       Elements elements = doc.select(".iuscp.isv");
       List<Picture> list = new ArrayList<>();
       for (Element element : elements) {
           //取照片地址
           String m = element.select(".iusc").get(0).attr("m");
           Map<String,Object> map = JSONUtil.toBean(m, Map.class);
           String murl =(String) map.get("murl");
           //取标题
           String title = element.select(".inflnk").get(0).attr("aria-label");
           Picture picture = new Picture();
           picture.setUrl(murl);
           picture.setTitle(title);
           list.add(picture);
       }
       System.out.println(list);
   }
    @Test
    void testcrawerTest(){
//        分析数据源，怎么获取？
        //1.获取数据
        String json="{\"current\":1,\"pageSize\":8,\"sortField\":\"createTime\",\"sortOrder\":\"descend\",\"category\":\"文章\",\"reviewStatus\":1}";
        String  url = "https://www.code-nav.cn/api/post/search/page/vo";
        String result = HttpRequest.post(url)
                .body(json)
                .execute()
                .body();
        //2.json转对象
        Map<String,Object> map= JSONUtil.toBean(result,Map.class);
        JSONObject data =(JSONObject) map.get("data");
        JSONArray records =(JSONArray) data.get("records");
        List<Post> postList=new ArrayList<>();
        for (Object record : records) {
            JSONObject tempRecord=(JSONObject) record;
            Post post = new Post();
            post.setContent(tempRecord.getStr("content"));
            JSONArray tags =(JSONArray) tempRecord.get("tags");
            List<String> tagsList = tags.toList(String.class);
            post.setTags(JSONUtil.toJsonStr(tagsList));
            post.setUserId(1l);
            post.setTitle(tempRecord.getStr("title"));
            postList.add(post);
        }

//        拿到数据后，怎么处理？
//        写入数据库等存储
        boolean b = postService.saveBatch(postList);
        Assertions.assertTrue(b);
    }

}
