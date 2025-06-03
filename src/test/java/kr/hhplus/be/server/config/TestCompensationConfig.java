//package kr.hhplus.be.server.config;
//
//import kr.hhplus.be.server.application.order.CreateOrderCommand;
//import kr.hhplus.be.server.application.order.OrderCompensationService;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.context.annotation.Profile;
//
//import java.util.List;
//
//@Configuration
//@Profile("test")
//public class TestCompensationConfig {
//
//    @Bean
//    @Primary
//    public OrderCompensationService stubOrderCompensationService() {
//        return new OrderCompensationService(null, null) {
//            @Override
//            public void compensateStock(List<CreateOrderCommand.OrderItemCommand> items) {
//                System.out.println("[보상 트랜잭션] 재고 복구 시작 (테스트용 stub)");
//                // no-op: 테스트용 stub
//            }
//
//            @Override
//            public void markOrderAsFailed(String orderId) {
//                System.out.println("[보상 트랜잭션] 주문 상태 변경 시작 (테스트용 stub) - 주문 ID: " + orderId);
//                // no-op: 테스트용 stub
//            }
//        };
//    }
//}
