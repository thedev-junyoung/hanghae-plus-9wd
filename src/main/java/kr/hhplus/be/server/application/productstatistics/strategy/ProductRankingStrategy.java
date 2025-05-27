package kr.hhplus.be.server.application.productstatistics.strategy;


import java.time.Duration;

public interface ProductRankingStrategy {

    void record(Long productId, int quantity);
    void undo(Long productId, int quantity);
    String getPrefix();
    Duration getExpireDuration();
}