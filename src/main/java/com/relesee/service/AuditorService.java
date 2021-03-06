package com.relesee.service;

import com.relesee.dao.AuditorDao;
import com.relesee.dao.UserDao;
import com.relesee.domains.Result;
import com.relesee.domains.User;
import com.relesee.utils.FileUtil;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuditorService {

    private static final Logger logger = Logger.getLogger(AuditorService.class);

    @Autowired
    AuditorDao auditorDao;

    @Autowired
    UserDao userDao;

    @Value("#{projectProperties['server.host']}")//projectProperties是在spring的配置文件中配置的一个bean
    private String SERVER_HOST;

    public Result<String> changeHeadPhoto(MultipartFile headPhoto, String userId, String realpath){
        Result<String> result = new Result();
        String DIRECTORY = realpath+"img/userHeadPhotos/"+userId;
        String PHOTO_URI = realpath+"img/userHeadPhotos/"+userId+"/"+headPhoto.getOriginalFilename();
        String PHOTO_WEB_URL = SERVER_HOST+"/img/userHeadPhotos/"+userId+"/"+headPhoto.getOriginalFilename();
        boolean fileWriteSuccess;
        try {
            FileUtil.createDirIfNotExist(DIRECTORY);
            FileUtil.writeInputStreamToDirectory(headPhoto.getInputStream(), PHOTO_URI);
            fileWriteSuccess = true;
        } catch (Exception e){
            logger.error("审核员在更改头像上传文件时发生错误", e);
            fileWriteSuccess = false;
        }
        int count = 0;
        if (fileWriteSuccess){
            count = auditorDao.updateHeadPhoto(PHOTO_WEB_URL, userId);
        }

        if (count == 1){
            result.setFlag(true);
            result.setMessage("头像更改成功");
            result.setResult(PHOTO_WEB_URL);
            ((User) SecurityUtils.getSubject().getPrincipal()).setHeadPhoto(PHOTO_WEB_URL);
        } else {
            result.setMessage("头像更改出错");
        }
        return result;
    }

    public Result<User> updatePersonalInformation(User user){
        String userId = ((User) SecurityUtils.getSubject().getPrincipal()).getUserId();
        user.setUserId(userId);
        Result result = new Result();
        int count = userDao.updatePersonalInformation(user);
        if (count == 1){
            result.setFlag(true);
            result.setMessage("更新个人信息成功");
            ((User) SecurityUtils.getSubject().getPrincipal()).setEmail(user.getEmail());
            ((User) SecurityUtils.getSubject().getPrincipal()).setPhone(user.getPhone());
            result.setResult(SecurityUtils.getSubject().getPrincipal());
        } else {
            result.setFlag(false);
            result.setMessage("更新个人信息出错");
        }
        return result;
    }

    public Result<User> updatePassword(String oldPassword,String newPassword){
        Result result = new Result();
        String password = ((User) SecurityUtils.getSubject().getPrincipal()).getPassword();
        String userId = ((User) SecurityUtils.getSubject().getPrincipal()).getUserId();
        if (!password.equals(oldPassword)){
            result.setFlag(false);
            result.setMessage("修改密码失败，输入的原密码不正确");
        } else {
            int count = userDao.updatePassword(userId, newPassword);
            if (count == 1){
                result.setFlag(true);
                result.setMessage("修改密码成功！");
                password = newPassword;
                result.setResult(SecurityUtils.getSubject().getPrincipal());
            } else {
                result.setFlag(false);
                result.setMessage("修改密码出错");
            }
        }
        return result;
    }

}
