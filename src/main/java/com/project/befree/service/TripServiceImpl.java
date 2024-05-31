package com.project.befree.service;

import com.project.befree.domain.Place;
import com.project.befree.domain.Trip;
import com.project.befree.domain.Member;
import com.project.befree.dto.PlanRequestDTO;
import com.project.befree.dto.TripListResponseDTO;
import com.project.befree.dto.TripRequestDTO;
import com.project.befree.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripServiceImpl implements TripService{

    private final TripRepository tripRepository;
    private final MemberServiceImpl memberService;

    @Override
    public Long add(String email, TripRequestDTO tripRequestDTO) {
        Member member = memberService.getOne(email);

        Trip newTrip = Trip.builder()
                .ttitle(tripRequestDTO.getTtitle())
                .tbegin(tripRequestDTO.getTbegin())
                .tend(tripRequestDTO.getTend())
                .tregion(tripRequestDTO.getTregion())
                .member(member)
                .build();
        Trip savedTrip = tripRepository.save(newTrip);
        return savedTrip.getTid();
    }

    @Override
    public TripListResponseDTO list(String email, int page) {
        log.info("************* TripServiceImpl.java / method name : list / email : {}", email);
        List<Trip> allTripByEmail = tripRepository.findAllByEmail(email);
        Collections.reverse(allTripByEmail);

        // 페이지네이션 적용
        int fromIndex = (page - 1) * 5; // 페이지 index 구하기 / 첫페이지 : 0
        int toIndex = Math.min(fromIndex + 5, allTripByEmail.size()); // 전체와 현재 페이지의 길이 비교 더 작은 값 남음

        if (fromIndex > allTripByEmail.size()) {
            return TripListResponseDTO.builder()
                    .paginatedTrips(Collections.emptyList())
                    .totalPage((allTripByEmail.size()+4)/5) // 페이지 수 계산
                    .build(); // 페이지가 범위를 벗어나면 빈 리스트 반환
        }

        List<Trip> paginatedTrips = allTripByEmail.subList(fromIndex, toIndex);

        TripListResponseDTO tripListResponseDTO = TripListResponseDTO.builder()
                .paginatedTrips(paginatedTrips)
                .totalPage((allTripByEmail.size()+4)/5) // 페이지 수 계산
                .build();


        return tripListResponseDTO;
    }

    @Override
    public boolean delete(Long tid) {
        try{
            tripRepository.deleteById(tid);
            return true;
        }catch (Exception e){
            log.info("************* TripServiceImpl.java / method name : delete / e : {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Place> getPlan(Long tid) {
        Optional<Trip> tripOptional = tripRepository.findById(tid);
        if(tripOptional.isPresent()){
            Trip trip = tripOptional.get();
            log.info("************* TripServiceImpl.java / method name : getPlan / trip : {}", trip);
            return trip.getPlaceList();
        }else{
            log.info("************* TripServiceImpl.java / method name : getPlan / Plan return is null");
            return null;
        }
    }

    @Override
    public boolean putPlan(Long tid, PlanRequestDTO planRequestDTO) {
        Optional<Trip> tripOptional = tripRepository.findById(tid);
        if(tripOptional.isPresent()){
            Trip newTrip = tripOptional.get().replace(planRequestDTO.getPlanList());
            tripRepository.save(newTrip);
            return true;
        }else{
            log.info("************* TripServiceImpl.java / method name : patchPlan / tripOptional : Trip return is null");
            return false;
        }
    }

    @Override
    public boolean addPlace(Long tid, PlanRequestDTO planRequestDTO) {
        Optional<Trip> tripOptional = tripRepository.findById(tid);
        if(tripOptional.isPresent()){
            Trip trip = tripOptional.get();
            List<Place> originPlaceList = trip.getPlaceList();

            // Stream.of : 가변인자 받아 스트림 생성 (두 개) / flatMap : 각 요소를 매핑하고 하나로 합침 / (List::stream) : 각 리스트를 스트림으로 변환
            List<Place> combinedPlaceList = Stream.of(originPlaceList, planRequestDTO.getPlanList())
                    .flatMap(List::stream)
                    .toList();

            Trip replacedPlaceList = trip.replace(combinedPlaceList);
            log.info("************* TripServiceImpl.java / method name : addPlace / replacedPlaceList : {}", replacedPlaceList);
            tripRepository.save(replacedPlaceList);
            return true;
        }else{
            log.info("************* TripServiceImpl.java / method name : patchPlan / tripOptional : Trip return is null");
            return false;
        }
    }
}
