# 롤프랜드 - 함께할 게임친구 만들기
![previewImage]()
### 1. 작업인원  
총 3인 중 개발전담
### 2. 작업기간  
2020년 3월~2020년 7월(4개월)
### 3. 개발환경  
Android Studio & Firebase
### 4. 개발배경  
게임시장 1순위 게임인 리그오브레전드에서 같이 게임 할 친구를 구하기 위한 커뮤니케이션 앱의 필요성을 위해서 개발
### 5. 주요기능  
- 자신의 게임 닉네임을 기반으로 계정 생성 및 로그인(+자동로그인)
- 단체채팅방 및 개인채팅방 생성 가능
- 메세지 푸시알림 가능
- 실시간으로 상대방의 메세지 확인 가능
- 관리자에게 메일로 문의하기 가능
- 특정 작업시 전면 광고 호출
### 6. 배운 기술  
- Firebase Authentication을 이용하여 이메일/비밀번호로 로그인
- 로그인시 Firebase RealtimeDatabase의 데이터를 읽어 다중 단말 중복 로그인 방지
- 회원가입시 회원정보를 UserModel에 저장하여 Firebase RealtimeDatabase에 저장
- ConnectivityManager를 이용하여 현재 인터넷 연결 상태 체크
- BottomNavigationView와 Fragment를 이용한 Activity 화면 분할
- Firebase RealtimeDatabase의 데이터를 읽어 각 프래그먼트의 RecyclerView 갱신
- 메세지 전송 시 Gson과 OkHttpClient를 이용하여 상대방에게 푸시 전송
- 구글의 smtp를 이용하여 문의 메일 전송 가능
