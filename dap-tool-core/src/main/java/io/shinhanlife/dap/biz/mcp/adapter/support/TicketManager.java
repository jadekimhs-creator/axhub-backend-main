package io.shinhanlife.dap.biz.mcp.adapter.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @package io.shinhanlife.dap.biz.mcp.adapter.support
 * @className TicketManager
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
@Slf4j
@Component
public class TicketManager {

    // 로컬 시뮬레이션을 위한 인메모리 저장소 (운영 환경에서는 Redis 등을 사용)
    private final Map<String, Ticket> ticketStore = new ConcurrentHashMap<>();
    
    // 가짜 EAI 작업(10초 대기)을 실행할 백그라운드 쓰레드 풀
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    /**
     * 새로운 비동기 작업을 접수하고 티켓을 발급합니다.
     */
    public String issueTicket(String interfaceId, String payload) {
        String ticketId = "TICKET-" + UUID.randomUUID().toString();
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(ticketId);
        ticket.setStatus("PROCESSING");
        ticket.setMessage("EAI 시스템에서 데이터 처리 중입니다...");
        
        ticketStore.put(ticketId, ticket);
        
        log.info(" [TicketManager] 비동기 작업 티켓 발급 완료: {}", ticketId);
        
        // 10초 뒤에 자동으로 작업을 완료 상태로 변경하는 백그라운드 시뮬레이터 실행
        simulateEaiProcessing(ticketId, interfaceId);
        
        return ticketId;
    }

    /**
     * 특정 티켓의 현재 상태를 조회합니다.
     */
    public Ticket getTicketStatus(String ticketId) {
        return ticketStore.getOrDefault(ticketId, new Ticket("NOT_FOUND", "해당 티켓을 찾을 수 없습니다."));
    }

    /**
     * 10초 뒤에 상태를 COMPLETED로 변경하여 진짜 EAI 배치가 끝난 것처럼 흉내냅니다.
     */
    private void simulateEaiProcessing(String ticketId, String interfaceId) {
        scheduler.schedule(() -> {
            Ticket ticket = ticketStore.get(ticketId);
            if (ticket != null) {
                ticket.setStatus("COMPLETED");
                ticket.setMessage("EAI 배치가 정상적으로 완료되었습니다.");
                // 가짜 최종 결과 데이터 주입
                ticket.setResultData("{\"resultCode\":\"0000\", \"interfaceId\":\"" + interfaceId + "\", \"processedRecords\":50000}");
                ticketStore.put(ticketId, ticket);
                log.info(" [TicketManager] EAI 비동기 작업 시뮬레이션 완료! (Ticket: {})", ticketId);
            }
        }, 10, TimeUnit.SECONDS); // 10초 지연
    }

    // 티켓 상태를 담을 내부 DTO 클래스
    @lombok.Data
    public static class Ticket {
        private String ticketId;
        private String status; // PROCESSING, COMPLETED, FAILED, NOT_FOUND
        private String message;
        private String resultData; // 최종 완료 시 담길 데이터
        
        public Ticket() {}
        public Ticket(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}
