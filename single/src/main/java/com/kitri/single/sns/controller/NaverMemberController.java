package com.kitri.single.sns.controller;

import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.kitri.single.sns.model.SnsDto;
import com.kitri.single.sns.service.NaverLoginService;
import com.kitri.single.sns.service.NaverLoginServiceImpl;
import com.kitri.single.user.model.UserDto;
import com.kitri.single.util.Utill;

@Controller
@SessionAttributes("userInfo")
@RequestMapping(value = "/navermember")
public class NaverMemberController {
	Logger logger = LoggerFactory.getLogger(NaverMemberController.class);

	@Autowired
	NaverLoginService naverLoginService;

	APIMemberProfile apiMemberProfile = new APIMemberProfile();

	//네아로 로그인 눌렀을시 callback.jsp를 통해 호출되는 메소드
	@RequestMapping(value = "/callback", method = RequestMethod.POST)
	public String callback(@RequestParam Map<String, String> parameter, Model model, HttpServletRequest request) {
		// TODO sns에 따라 회원가입할지 기존아이디와 연동할지 테스트 필요
		// 소셜 로그인 아이디 수신
		// 첫 로그인
		// -> 새로운페이지 오픈 -> 기존아이디와 연동? 새로 회원가입?
		// 기존 소셜아이디 존재
		// -> 해당 아이디로 로그인 진행
		String snsEmail = parameter.get("email");// 인증시 필수입력값
		String accessToken = parameter.get("accessToken");
		
		
		// 회원정보 얻기
		String userProfile = apiMemberProfile.getMemberProfile(accessToken);

		JSONObject resultJson = new JSONObject(userProfile);
		JSONObject profileObj = (JSONObject) resultJson.get("response");
		//sns 프로필정보
		String snsId = Utill.getStringJson(profileObj, "id");
		String userNickname = Utill.getStringJson(profileObj, "nickname");
		String userAge= Utill.getStringJson(profileObj, "age");
		String userGender = Utill.getStringJson(profileObj, "gender");
		String userName = Utill.getStringJson(profileObj, "name");
		String userBirthday = Utill.getStringJson(profileObj, "birthday");
		String userProfile_image = Utill.getStringJson(profileObj, "profile_image");
		
		SnsDto snsDto = new SnsDto();
		snsDto.setSnsEmail(snsEmail);
		snsDto.setSnsType("naver");
		
		SnsDto oldSnsDto = naverLoginService.getSnsLogin(snsDto);
		
		UserDto userDto= new UserDto();
		userDto.setUserId(snsEmail);
		userDto = naverLoginService.getUser(userDto);
		
		logger.debug("callback>>>userDto: "+userDto);
		logger.debug("callback>>>oldSnsDto: "+oldSnsDto);
	
		if(userDto!= null && oldSnsDto == null) {
			//이 아이디로 회원가입한 적이 있습니다, 소셜로는 첫 로그인 
			//1. snsDto 생성 및 연결
			//2. 세션 생성 
			//3. 메인페이지 이동
			snsDto = new SnsDto();
			snsDto.setSnsId(snsId);
			snsDto.setUserId(snsEmail);
			snsDto.setSnsEmail(snsEmail);
			snsDto.setSnsType("naver");
			snsDto.setSnsToken(accessToken);
			snsDto.setSnsConnectDate(Calendar.getInstance().getTime().toString());
			
			naverLoginService.registSns(snsDto);
			
			WebUtils.setSessionAttribute(request, "userInfo", userDto ); //리다이렉트시 세션은 이렇게 담아준다.
			userDto.setSnsDto(snsDto);
			model.addAttribute("userInfo",userDto);
			return "redirect:/index.jsp";
			
		}
		else if(userDto != null && oldSnsDto != null) {
			//이 아이디로 회원가입한 적이 있습니다, 소셜과 연결되어있음 
			//1. 세션 생성 
			//2. 메인페이지 이동		
			userDto.setSnsDto(snsDto);
//			model.addAttribute("userInfo",userDto);
			
			WebUtils.setSessionAttribute(request, "userInfo", userDto ); //리다이렉트시 세션은 이렇게 담아준다.
			return "redirect:/index.jsp";
		}
		else if (oldSnsDto == null) {
			logger.debug(">>>callback>>>snsDto null");
			//이소셜로는 처음 로그인 합니다. 
			userDto = new UserDto();
			userDto.setUserId(snsEmail);
			userDto.setUserName(userName);
			userDto.setUserNickname(userNickname);
			userDto.setUserGender(userGender);
			userDto.setUserBirthday(userBirthday);
			userDto.setUserProfile(userProfile_image);
			
			
			snsDto = new SnsDto();
			snsDto.setSnsId(snsId);
			snsDto.setUserId(snsEmail);
			snsDto.setSnsEmail(snsEmail);
			snsDto.setSnsType("naver");
			snsDto.setSnsToken(accessToken);
			snsDto.setSnsConnectDate(Calendar.getInstance().getTime().toString());
			
			userDto.setSnsDto(snsDto);

			model.addAttribute("userInfo", userDto);		
//			return "member/register/register";
		}
		logger.debug(">>>>> oldSnsDto.toString"+oldSnsDto.toString());
		logger.debug(">>>>> userDto.toString"+userDto.toString());
		logger.debug(">>>callback>>>else");
		return "index";
	} //callback

//	@RequestMapping(value = "/mvcallback", method = RequestMethod.GET)
//	public String mvcallback(@RequestParam Map<String, String> parameter, Model model, HttpServletRequest request) {
//		logger.info("--mvcallback");
//
//		String referer = request.getHeader("referer");
//		logger.info("referer:" + referer);
//		String access_token = parameter.get("access_token");
//		String state = parameter.get("state");
//		String token_type = parameter.get("token_type");
//		String expires_in = parameter.get("expires_in");
//
//		logger.info("access_token:" + access_token);
//		logger.info("state:" + state);
//		logger.info("token_type:" + token_type);
//		logger.info("expires_in:" + expires_in);
//
//		return "index";
//	}

}
