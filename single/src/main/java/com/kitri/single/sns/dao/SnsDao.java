package com.kitri.single.sns.dao;

import com.kitri.single.sns.model.SnsDto;
import com.kitri.single.user.model.UserDto;

public interface SnsDao {
	public SnsDto getSnsLogin(SnsDto snsDto);
	
	//2019-07-08 백업
	public UserDto getUser(String snsEmail);
	public int register(SnsDto naverUserDto);
	

}
