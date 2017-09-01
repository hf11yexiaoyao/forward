package com.ntech.controller;

import com.ntech.exception.PermissionDeniedException;
import com.ntech.forward.MethodUtil;
import com.ntech.forward.PutUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/")
public class ForwardController {

    @Autowired
    private HttpServletRequest request;

    private final static Logger logger = Logger.getLogger(ForwardController.class);

    @RequestMapping("/n-tech/**")
    @ResponseBody
    public String methodHandler(HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes)
            throws ServletException, IOException {
//        HashMap map = (HashMap) redirectAttributes.getFlashAttributes();
        if(redirectAttributes.containsAttribute("request")&&redirectAttributes.containsAttribute("response")){
            request = (HttpServletRequest) redirectAttributes.getFlashAttributes().get("request");
            response = (HttpServletResponse) redirectAttributes.getFlashAttributes().get("response");

        }
//        HttpServletRequest request1= redirectAttributes.getFlashAttribute("request");
        String reply="";
//        PrintWriter out = response.getWriter();
        String method = request.getMethod();
        if(method.equals("PUT")||method.equals("DELETE")){
            reply = PutUtil.requestForword(request,response);
        }else{
            reply = MethodUtil.getInstance().requestForword(request, response);
        }
        if(reply.equals(""))
            reply="BAD_INPUT";
        return reply;
    }
}
