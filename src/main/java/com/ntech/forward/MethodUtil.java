package com.ntech.forward;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;

import com.ntech.exception.IllegalGalleryException;
import com.ntech.util.Check;
import com.ntech.util.ConfigManager;
import com.ntech.util.Base64Encrypt;
import com.ntech.util.ErrorPrompt;

public class MethodUtil {
	private static MethodUtil instance;
	private final Logger logger = Logger.getLogger(MethodUtil.class);
	private final Check check = (Check) Constant.GSB.getBean("check");

	private static FileItemFactory factory = new DiskFileItemFactory();
	private static Map<String, String> header = new HashMap<String, String>();
	private static Map<String, String> param = new HashMap<String, String>();
	private static Map<String, Object> file = new HashMap<String, Object>(2);

	private List<String> galleries = null;


	private MethodUtil() {
	}

	public static MethodUtil getInstance() {    //对获取实例的方法进行同步
		if (instance == null) {
			synchronized (MethodUtil.class) {
				if (instance == null)
					instance = new MethodUtil();
			}
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	public String requestForword(HttpServletRequest request,
								 HttpServletResponse response) {
		logger.info("*************START***********");
		String SDKreply;
		String meta = "no";
		String API = (String) request.getAttribute("API");
		String api = (String) request.getAttribute("api");
		header.clear();
		param.clear();
		file.clear();
		String userName = (String)request.getAttribute("userName");
		if (api != null && api.equals("face/post")) {
			galleries = (List<String>) request.getAttribute("galleries");
			String inputGalleries = request.getParameter("galleries");
			if (galleries.size() != 0 && (inputGalleries == null || inputGalleries.equals("")))
				param.put("galleries", userName);
		}
		if ("allGalleries".equals(API)) {
			String reply = JSONArray.toJSONString((List<String>) request.getAttribute("allGalleries"));
			return reply.replaceAll("_anytec_"+userName,"");
		}
		//判断是否有文件输入
		boolean isMultiPart = ServletFileUpload.isMultipartContent(request);
		if (isMultiPart) {
			logger.info("********* HAS FILE********");
			meta = "isFile";

			ServletFileUpload upload = new ServletFileUpload(factory);
			try {
				List<FileItem> items = upload.parseRequest(request);
				Iterator<FileItem> iterator = items.iterator();
				while (iterator.hasNext()) {
					FileItem item = iterator.next();
					if (item.isFormField()) {
						//文本
						String filedName = item.getFieldName();
						String value = new String(item.getString().getBytes("iso-8859-1"));
						if (filedName.equals("galleries") || filedName.equals("gallery")) {
							logger.info("inputGaleries :" + value);
							filedName = "galleries";
							StringBuilder galleryValue = new StringBuilder();
							checkInputGalleries(value,userName,galleryValue);
							param.put(filedName,galleryValue.toString());
						}else {
							param.put(filedName, value);
						}
						logger.info("文本域：" + filedName + "——" + value);
						//有上传文件
					} else {
						file.put("contentType", item.getContentType());
						logger.info("contentType:" + item.getContentType());
						String filedName = item.getFieldName();
						String fileName = item.getName();
						logger.info("文件控件: " + filedName + "--" + fileName);
						if (item.getSize() > 1024 * 1024 * 10) {
							logger.info("*******上传图片过大*******");
						}
						byte[] pic = item.get();
						logger.info("PICTURE.LENGTH:" + pic.length);

						file.put(filedName, pic);
					}
				}
			} catch (FileUploadException e) {
				response.setStatus(500);
				logger.error("*****FILE_UPLOAD_FAIL*****@" + request.getAttribute("userName"));
				ErrorPrompt.addInfo("error", "file upload fail");
				logger.error(e.getMessage());
				e.printStackTrace();

				return null;
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
				return null;
			}
		}
		//获取表单文本数据
		Enumeration<String> enumeration = request.getParameterNames();
		while (enumeration.hasMoreElements()) {
			logger.info("*********HAS INPUT*********");
			meta = "yes";
			String name = enumeration.nextElement();
			if ((name.equals("max_id") || name.equals("min_id")) && request.getMethod().equals("GET")) {
				meta = "no";
			} else {
				param.put(name, request.getParameter(name));
			}
			logger.info("PARAM:" + name + "--" + "" + param.get(name));
		}
		//转发和设定报头信息
		header.put("Method", request.getMethod());

		String localAPI = (String) request.getAttribute("localAPI");
		if (localAPI == null || localAPI.equals("")) {
			header.put("API", API);
		} else {
			header.put("API", localAPI);
		}
		//判断
		String galleries = (String) request.getAttribute("personFaceInsert");
		if (galleries != null && !galleries.equals("")) {
			param.put("galleries", galleries);
		}

		logger.info(request.getAttribute("API").toString());
		logger.info("Token :" + request.getHeader("Token"));
		try {
			SDKreply = HttpUploadFile.getInstance().httpURLConnectionSDK(header, param, file, meta);
			response.setStatus(HttpUploadFile.status);
		} catch (IOException e) {
			ErrorPrompt.addInfo("code", "BAD_INPUT");
			response.setStatus(400);
			logger.error(e.getMessage() + "@" + userName);
			e.printStackTrace();
			return null;
		}
		response.setHeader("Cache-control", "no-cache");
		//计费信息
		String contype = check.getContype(userName);
		if(contype==null)
			try {
				throw new Exception("set meal contype is null");
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		if(HttpUploadFile.status==200&&request.getAttribute("chargeAPI")!=null) {
			String chargeAPI = (String) request.getAttribute("chargeAPI");
			String charge = (String) request.getAttribute("charge");

			if (contype.equals("times")) {
				int status = 0;
				boolean result = check.mealTimesCount(userName);
				if (result)
					status = 1;
				check.setLog(userName, chargeAPI + " ￥" + charge, status);
				logger.info("timesCount: " + result);
			} else {
				int status = 0;
				boolean result = check.mealDateCheck(userName);
				if (result)
					status = 1;
				check.setLog(userName, chargeAPI + " ￥" + charge, status);
				logger.info("dateCheck： "+result);
			}
		}
		if(contype.equals("date"))
			check.mealDateCheck(userName);
		if (localAPI == null || localAPI.equals("")) {
			String string = ConfigManager.getInstance().getParameter("PICTURE") + "/" + Base64Encrypt.encryptUserName((String) request.getAttribute("userName"));
			if (SDKreply != null && !"".equals(SDKreply)) {
				//包装响应FindfaceSDK响应（转发图片，包装库名）
				return SDKreply.replaceAll("_anytec_"+userName,"").replaceAll("http://127.0.0.1:3333/uploads", string);
			}
		}
		return SDKreply;
	}


	//对用户输入的galleries参数进行权限检查
	private void checkInputGalleries(String value, String userName,StringBuilder galleryValue) {
		boolean existGallery;
		if (galleries != null && galleries.size() != 0) {
			logger.info("*******USER_HOLD_GALLERY*******");
			try {
				if (value.contains(",")) {
					String[] inputGalleries;
					logger.info("inputMoreGalleries");
					inputGalleries = value.split(",");
					for (String inputGallery : inputGalleries) {
						logger.info(inputGallery);
						inputGallery = new StringBuilder(inputGallery).append("_anytec_" + userName).toString();
						existGallery = galleries.contains(inputGallery);
						logger.info(inputGallery+" (CHECK_RESULT) :" + existGallery);
						if (!existGallery)
							throw new IllegalGalleryException("bad_galleries");
						galleryValue.append(inputGallery);
					}
				} else {
					logger.info("inputOneGallery");
					existGallery = galleries.contains(value);
					if (!value.equals(userName))
						galleryValue.append(value).append("_anytec_" + userName).append(",");
					galleryValue.append(userName);
					logger.info(value+" (CHECK_RESULT) :" + existGallery);
				}
			}catch (IllegalGalleryException e) {
				logger.error("*****BAD_GALLERY*****@" + userName);
				ErrorPrompt.addInfo("error", "bad_gallery");
				e.printStackTrace();
			}
		}
	}
}