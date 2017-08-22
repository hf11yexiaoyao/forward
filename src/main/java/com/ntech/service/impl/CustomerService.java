package com.ntech.service.impl;

import com.ntech.dao.CustomerMapper;
import com.ntech.model.Customer;
import com.ntech.model.CustomerExample;
import com.ntech.service.inf.ICustomerService;
import com.ntech.util.MailUtil;
import com.ntech.util.SHAencrypt;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.util.Date;
import java.util.List;

@Service
public class CustomerService implements ICustomerService {

    private static final Logger logger = Logger.getLogger(CustomerService.class);

    @Autowired
    CustomerMapper customerMapper;

    @Transactional
    public int add(Customer customer) throws MessagingException {
        customer.setRegtime(new Date());
        sendEmail(customer);
        return customerMapper.insert(customer);
    }

    public int delete() {
        return 1;
    }

    public int modify() {
        return 1;
    }

    public List<Customer> findAll() {
        logger.info("find all customer");
        CustomerExample example = new CustomerExample();
        example.createCriteria().andNameIsNotNull();
        return customerMapper.selectByExample(example);
    }

    public Customer findByName(String name) {
        logger.info("get user name:"+name);
        Customer customer=null;
        CustomerExample example = new CustomerExample();
        example.createCriteria().andNameEqualTo(name);
        List<Customer> list = customerMapper.selectByExample(example);
        if(list.size()>0){
            customer=list.get(0);
        }
        return customer;
    }

    public boolean checkUserName(String userName) {
        logger.info("check user name");
        CustomerExample example = new CustomerExample();
        example.createCriteria().andNameEqualTo(userName);
        List<Customer> result = customerMapper.selectByExample(example);
        return result.size()>0?true:false;
    }

    public boolean loginCheck(String name, String password) {
        Customer customer= findByName(name);
        if(null != customer){
            if("0".equals(customer.getActive())){
                logger.warn(name+" user is not active");
                return false;
            }
            logger.info("check login info");
            try {
                password= SHAencrypt.encryptSHA(password);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CustomerExample example = new CustomerExample();
            example.createCriteria().andNameEqualTo(name).andPasswordEqualTo(password);
            List<Customer> result =customerMapper.selectByExample(example);
            return result.size()>0?true:false;
        }else{
            return false;
        }
    }

    private void sendEmail(Customer customer) throws MessagingException {
        String validateCode=SHAencrypt.encryptSHA(customer.getEmail());
        StringBuffer content=new StringBuffer("点击下面链接激活账号，48小时生效，否则重新注册账号，链接只能使用一次，请尽快激活！</br>");
        content.append("<a href=\"http://localhost:8080/customer/active?email=");
        content.append(customer.getEmail());
        content.append("&name=");
        content.append(customer.getName());
        content.append("&validateCode=");
        content.append(validateCode);
        content.append("\">http://localhost:8080/customer/active?action=activate&email=");
        content.append(customer.getEmail());
        content.append("&validateCode=");
        content.append(validateCode);
        MailUtil.send_mail(customer.getEmail(),content.toString());
    }
}
