package com.yrw.im.rest.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yrw.im.common.domain.po.User;
import com.yrw.im.common.exception.ImException;
import com.yrw.im.rest.web.mapper.UserMapper;
import com.yrw.im.rest.web.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Date: 2019-04-07
 * Time: 18:36
 *
 * @author yrw
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public Mono<Long> saveUser(String username, String pwd) {
        User user = new User();
        user.setUsername(username);
        user.setSalt(RandomStringUtils.randomAscii(16));
        user.setPwdHash(DigestUtils.sha256Hex(pwd + user.getSalt()));
        return Mono.fromSupplier(() -> save(user) ? user.getId() : null)
            .switchIfEmpty(Mono.error(new ImException("[rest] save user info failed")))
            .onErrorMap(e -> new ImException("[rest] username exist"));
    }

    @Override
    public Mono<User> verifyAndGet(String username, String pwd) {
        return Mono.fromSupplier(() -> getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)))
            .map(user -> verityPassword(pwd, user.getSalt(), user.getPwdHash()) ? user : null);
    }

    private boolean verityPassword(String pwdSha, String salt, String pwdHash) {
        String hashRes = DigestUtils.sha256Hex(pwdSha + salt);
        return hashRes.equals(pwdHash);
    }
}
