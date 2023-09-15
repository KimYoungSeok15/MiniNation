package com.ssafy.mini.domain.member.service;

import com.ssafy.mini.domain.master.entity.Master;
import com.ssafy.mini.domain.master.repository.MasterRepository;
import com.ssafy.mini.domain.member.dto.request.MemberJoinRequest;
import com.ssafy.mini.domain.member.dto.request.MemberLoginRequest;
import com.ssafy.mini.domain.member.dto.response.MemberLoginResponse;
import com.ssafy.mini.domain.member.entity.Member;
import com.ssafy.mini.domain.member.mapper.MemberMapper;
import com.ssafy.mini.domain.member.repository.MemberRepository;
import com.ssafy.mini.global.exception.ErrorCode;
import com.ssafy.mini.global.exception.MNException;
import com.ssafy.mini.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;
    private final MasterRepository masterRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final MemberMapper memberMapper;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public void join(MemberJoinRequest memberJoinRequest) {
        log.info("Service Layer::join() called");
        Member member = memberMapper.memberJoinRequestToMember(memberJoinRequest);

        idCheck(member.getMemId()); // 아이디 중복 검사
        member.changePwd(encodePassword(member.getMemPwd())); // 비밀번호 암호화

        // 회원 타입 저장
        String memberCode = masterRepository.findCodeByExpression(memberJoinRequest.getType());
        member.setMemType(masterRepository.findById(memberCode)
                .orElseThrow(() -> new MNException(ErrorCode.NO_SUCH_CODE)));

        member.setCardNo(generateCardNumber()); // 카드 번호 랜덤 생성
        memberRepository.save(member);
    }

    @Override
    public void idCheck(String id) {
        log.info("Service Layer::idCheck() called");

        if (memberRepository.existsByMemId(id)) {
            throw new MNException(ErrorCode.DUPLICATED_ID);
        }
    }

    @Override
    public MemberLoginResponse login(MemberLoginRequest memberLoginRequest) {
        log.info("Service Layer::login() called");

        Member member = memberRepository.findByMemId(memberLoginRequest.getId())
                .orElseThrow(() -> new MNException(ErrorCode.NO_SUCH_MEMBER));

        // 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(memberLoginRequest.getPassword(), member.getMemPwd())) {
            throw new MNException(ErrorCode.WRONG_PASSWORD);
        }

        // 토큰 발급
        String type = member.getMemType().getExpression();
        String accessToken = jwtProvider.generateAccessToken(member.getMemId());
        String refreshToken = jwtProvider.generateRefreshToken(member.getMemId());

        return MemberLoginResponse.builder()
                .memType(type)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void update(MemberJoinRequest memberJoinRequest) {

    }

    /**
     * 비밀번호 암호화
     * @param password 비밀번호
     * @return 암호화된 비밀번호
     */
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * 카드 번호 랜덤 생성
     * @return 카드 번호
     */
    private String generateCardNumber() {
        StringBuilder cardNumber = new StringBuilder();
        Random rnd = new Random();

        for (int i = 0; i < 16; ++i) {
            cardNumber.append(rnd.nextInt(10));
        }
        return cardNumber.toString();
    }


}
