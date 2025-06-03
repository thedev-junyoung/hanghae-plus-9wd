package kr.hhplus.be.server.infrastructure.coupon;


import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponException;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {
    private final CouponJpaRepository jpaRepository;

    @Override
    public void save(Coupon coupon) {
        jpaRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return jpaRepository.findByCode(code);
    }

    @Override
    public Coupon findByCodeForUpdate(String code) {
        return jpaRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new CouponException.NotFoundException(code));
    }

    @Override
    public Collection<Coupon> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Collection<Coupon> findExpiredCoupons() {
        return jpaRepository.findExpiredCoupons(LocalDateTime.now());
    }

    @Override
    public boolean wasIssued(long l, String successCase) {

        return jpaRepository.existsByIdAndCode(l, successCase);
    }

    @Override
    public boolean hasIssued(long l, String successCase) {
        return jpaRepository.existsByIdAndCode(l, successCase);
    }

}
