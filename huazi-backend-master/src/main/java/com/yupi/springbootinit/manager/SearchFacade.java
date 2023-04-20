package com.yupi.springbootinit.manager;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.dataSource.*;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.model.dto.post.PostQueryRequest;
import com.yupi.springbootinit.model.dto.search.SearchRequest;
import com.yupi.springbootinit.model.dto.user.UserQueryRequest;
import com.yupi.springbootinit.model.entity.Picture;
import com.yupi.springbootinit.model.enums.SearchTypeEnum;
import com.yupi.springbootinit.model.vo.PostVO;
import com.yupi.springbootinit.model.vo.SearchVo;
import com.yupi.springbootinit.model.vo.UserVO;
import com.yupi.springbootinit.service.PictureService;
import com.yupi.springbootinit.service.PostService;
import com.yupi.springbootinit.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Component
@Slf4j
public class SearchFacade {
    @Resource
    private UserDataSource userDataSource;
    @Resource
    private PictureDataSource pictureDataSource;
    @Resource
    private PostDataSource postDataSource;
    @Resource
    private DataSourceRegistry dataSourceRegistry;

    public SearchVo searchAll(@RequestBody SearchRequest searchRequest, HttpServletRequest request) {
        String type = searchRequest.getType();
        SearchTypeEnum enumByValue = SearchTypeEnum.getEnumByValue(type);
        ThrowUtils.throwIf(StringUtils.isBlank(type), ErrorCode.NOT_FOUND_ERROR);
        String searchText = searchRequest.getSearchText();
        long current = searchRequest.getCurrent();
        long pageSize = searchRequest.getPageSize();
        if (enumByValue == null) {
            //并发执行
            CompletableFuture<Page<Picture>> pictureTask = CompletableFuture.supplyAsync(() -> {
                Page<Picture> picturePage = pictureDataSource.doSearch(searchText, 1, 10);
                return picturePage;
            });
            CompletableFuture<Page<UserVO>> userTask = CompletableFuture.supplyAsync(() -> {
                UserQueryRequest userQueryRequest = new UserQueryRequest();
                userQueryRequest.setUserName(searchText);
                Page<UserVO> userVOPage = userDataSource.doSearch(searchText,current,pageSize);
                return userVOPage;
            });
            CompletableFuture<Page<PostVO>> postTask = CompletableFuture.supplyAsync(() -> {
                PostQueryRequest postQueryRequest = new PostQueryRequest();
                postQueryRequest.setSearchText(searchText);
                Page<PostVO> postVOPage = postDataSource.doSearch(searchText,current,pageSize);
                return postVOPage;
            });
            CompletableFuture.allOf(userTask, postTask, pictureTask).join();
            Page<UserVO> userVOPage = null;
            try {
                userVOPage = userTask.get();
                Page<PostVO> postVOPage = postTask.get();
                Page<Picture> picturePage = pictureTask.get();
                SearchVo searchVo = new SearchVo();
                searchVo.setPictureList(picturePage.getRecords());
                searchVo.setPostList(postVOPage.getRecords());
                searchVo.setUserList(userVOPage.getRecords());
                return searchVo;
            } catch (Exception e) {
                log.error("查询异常", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询异常");
            }
        } else {
            SearchVo searchVo = new SearchVo();
            DataSource<?> dataSource = dataSourceRegistry.getDataSourceByType(type);
            Page<?> page = dataSource.doSearch(searchText, current, pageSize);
            searchVo.setDataList(page.getRecords());
            return searchVo;
//            switch (searchTypeEnum) {
//                case POST:
//                    PostQueryRequest postQueryRequest = new PostQueryRequest();
//                    postQueryRequest.setSearchText(searchText);
//                    Page<PostVO> postVOPage = postService.listPostVoByPage(postQueryRequest, request);
//                    searchVo.setPostList(postVOPage.getRecords());
//                    break;
//                case USER:
//                    UserQueryRequest userQueryRequest = new UserQueryRequest();
//                    userQueryRequest.setUserName(searchText);
//                    Page<UserVO> userVOPage = userService.listUserVoByPage(userQueryRequest);
//                    searchVo.setUserList(userVOPage.getRecords());
//                    break;
//                case PICTURE:
//                    Page<Picture> picturePage = pictureService.searchPicture(searchText, 1, 10);
//                    searchVo.setPictureList(picturePage.getRecords());
//                    break;
//                default:
//            }
        }
    }
}
