package com.imooc.gmall.config;

import com.alibaba.fastjson.JSON;
import com.imooc.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandle;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getParameter("newToken");
        if (token != null) {
            CookieUtil.setCookie(request, response, "token", token, WebConst.COOKIE_MAXAGE, false);
        }
        // 当用户访问非登录之后的页面，登录之后，继续访问其他业务模块时，url 并没有newToken，但是后台可能将token 放入了cookie 中！
        //路径中没有newToken
        if (token == null) {
            token = CookieUtil.getCookieValue(request, "token", false);
        }
        // 从cookie 中获取token，解密token！
        if (token != null) {
            // 开始解密token 获取nickName
            Map map = getUserMapByToken(token);
            // 取出用户昵称
            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName", nickName);

        }
        // 在拦截器中获取方法上的注解！
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 获取方法上的注解LoginRequire
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (methodAnnotation != null) {
            // 此时有注解 ，
            // 判断用户是否登录了？ 调用verify
            // http://passport.atguigu.com/verify?token=xxx&salt=x
            String salt = request.getHeader("X-forwarded-for");
            // 调用verify（）认证 http://passport.atguigu.com/verify
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            if ("success".equals(result)) {
                // 登录，认证成功！
                // 保存一下userId
                // 开始解密token 获取nickName
                Map map = getUserMapByToken(token);
                // 取出userId
                String userId = (String) map.get("userId");
                // 保存到作用域
                request.setAttribute("userId", userId);
                return true;
            } else {
                // 认证失败！并且 methodAnnotation.autoRedirect()=true; 必须登录
                if (methodAnnotation.autoRedirect()){
                    // 必须登录！跳转到页面！
                    // 京东：https://passport.jd.com/new/login.aspx?ReturnUrl=https%3A%2F%2Fwww.jd.com%2F
                    // 我们：http://passport.atguigu.com/index?originUrl=http%3A%2F%2Fitem.gmall.com%2F36.html
                    // 先获取到url
                    String requestURL  = request.getRequestURL().toString();
                    System.out.println("requestURL:"+requestURL); // http://item.gmall.com/36.html
                    // 将url 进行转换
                    // http%3A%2F%2Fitem.gmall.com%2F36.html
                    String encodeURL  = URLEncoder.encode(requestURL, "UTF-8");
                    System.out.println("encodeURL："+encodeURL); //  http%3A%2F%2Fitem.gmall.com%2F36.html
                    // http://passport.atguigu.com/index?originUrl=http%3A%2F%2Fitem.gmall.com%2F36.html
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;
                }

            }
        }

        return true;
    }

    private Map getUserMapByToken(String token) {
        // token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.XzRrXwDhYywUAFn-ICLJ9t3Xwz7RHo1VVwZZGNdKaaQ
        // 获取中间部分
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] decode = base64UrlCodec.decode(tokenUserInfo);
        String mapJson = null;
        try {
            mapJson = new String(decode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return JSON.parseObject(mapJson, Map.class);
    }
}
