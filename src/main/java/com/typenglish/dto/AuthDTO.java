package com.typenglish.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDTO {

    @Data
    public static class RegisterReq {
        @NotBlank @Size(min = 2, max = 20)
        private String username;
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 6, max = 32)
        private String password;
    }

    @Data
    public static class LoginReq {
        @NotBlank(message = "请输入用户名或邮箱")
        private String account;
        @NotBlank(message = "请输入密码")
        private String password;
    }

    @Data
    public static class TokenResp {
        private String token;
        private UserInfo user;
    }

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
    }
}
