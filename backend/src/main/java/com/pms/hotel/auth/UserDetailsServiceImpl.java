package com.pms.hotel.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.pms.hotel.user.User;
import com.pms.hotel.user.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;

	public UserDetailsServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		return userRepository.findByEmail(User.normalizeEmail(email))
			.map(AuthenticatedUser::new)
			.orElseThrow(() -> new UsernameNotFoundException("Aucun compte pour cet email"));
	}

}
