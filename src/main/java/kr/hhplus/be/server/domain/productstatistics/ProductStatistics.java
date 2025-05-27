package kr.hhplus.be.server.domain.productstatistics;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.vo.Money;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "product_statistics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStatistics {

    @EmbeddedId
    private ProductStatisticsId id;

    @Column(nullable = false)
    private int salesCount;

    @Column(nullable = false)
    private long salesAmount;

    public static ProductStatistics create(Long productId, LocalDate date) {
        return new ProductStatistics(new ProductStatisticsId(productId, date), 0, Money.wons(0));
    }

    public ProductStatistics(ProductStatisticsId id, int salesCount, Money salesAmount) {
        this.id = id;
        this.salesCount = salesCount;
        this.salesAmount = salesAmount.value();
    }

    public static ProductStatistics createOrUpdate(
            ProductStatistics existing,
            Long productId,
            LocalDate date,
            int quantity,
            Money unitPrice
    ) {
        ProductStatistics stat = (existing != null)
                ? existing
                : ProductStatistics.create(productId, date);
        stat.addSales(quantity, unitPrice);
        return stat;
    }



    public void accumulate(int quantity, Money unitPrice) {
        this.salesCount += quantity;
        this.salesAmount += unitPrice.multiply(quantity).value();
    }

    public void addSales(int quantity, Money unitPrice) {
        this.salesCount += quantity;
        this.salesAmount += unitPrice.multiply(quantity).value();
    }

    public Long getProductId() {
        return id.getProductId();
    }

    public LocalDate getStatDate() {
        return id.getStatDate();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductStatistics other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
