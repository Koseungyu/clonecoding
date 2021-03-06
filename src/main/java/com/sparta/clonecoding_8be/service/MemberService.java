package com.sparta.clonecoding_8be.service;


import com.sparta.clonecoding_8be.common.Constants;
import com.sparta.clonecoding_8be.common.exception.UserException;
import com.sparta.clonecoding_8be.dto.user.MemberRequestDto;
import com.sparta.clonecoding_8be.dto.user.MemberResponseDto;
import com.sparta.clonecoding_8be.dto.token.TokenDto;
import com.sparta.clonecoding_8be.dto.token.TokenRequestDto;
import com.sparta.clonecoding_8be.model.Member;
import com.sparta.clonecoding_8be.model.RefreshToken;
import com.sparta.clonecoding_8be.repository.MemberRepository;
import com.sparta.clonecoding_8be.repository.RefreshTokenRepository;
import com.sparta.clonecoding_8be.security.jwt.TokenProvider;
import com.sparta.clonecoding_8be.validator.LoginValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginValidator loginValidator;

    public Member findById(Long id) {
        Member user = memberRepository.findById(id).orElseThrow(
                ()-> new IllegalArgumentException("찾는 유저가 없습니다")
        );
        return user;
    }

    @Transactional
    public MemberResponseDto signup(MemberRequestDto memberRequestDto) throws UserException{
        if (memberRepository.existsByUsername(memberRequestDto.getUsername())) {
            throw new UserException(Constants.ExceptionClass.SIGNUP_USERNAME, HttpStatus.BAD_REQUEST, "이미 가입되어 있는 회원입니다.");
        }

        Member member = memberRequestDto.toMember(passwordEncoder);
        return MemberResponseDto.of(memberRepository.save(member));
    }

    public TokenDto login(MemberRequestDto memberRequestDto) throws UserException {
        Member member = loginValidator.isValidUsername(memberRequestDto.getUsername());
        loginValidator.isValidPassword(memberRequestDto.getPassword(), member.getPassword());
        // 1. Login ID/PW 를 기반으로 AuthenticationToken 생성
        UsernamePasswordAuthenticationToken authenticationToken = memberRequestDto.toAuthentication();
        // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        //    authenticate 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        tokenDto.setUsername(memberRequestDto.getUsername());

        tokenDto.setNickname(member.getNickname());
        tokenDto.setAddress(member.getAddress());

        // 4. RefreshToken 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .key(authentication.getName())
                .value(tokenDto.getRefreshToken())
                .build();

        refreshTokenRepository.save(refreshToken);

        // 5. 토큰 발급
        return tokenDto;
    }

    public TokenDto reissue(TokenRequestDto tokenRequestDto) {
        // 1. Refresh Token 검증
        if (!tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("Refresh Token 이 유효하지 않습니다.");
        }

        // 2. Access Token 에서 Member ID 가져오기
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. 저장소에서 Member ID 를 기반으로 Refresh Token 값 가져옴
        RefreshToken refreshToken = refreshTokenRepository.findByKey(authentication.getName())
                .orElseThrow(() -> new RuntimeException("로그아웃 된 사용자입니다."));

        // 4. Refresh Token 일치하는지 검사
        if (!refreshToken.getValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
        }

        // 5. 새로운 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 6. 저장소 정보 업데이트
        RefreshToken newRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        refreshTokenRepository.save(newRefreshToken);

        // 토큰 발급
        return tokenDto;
    }
}




