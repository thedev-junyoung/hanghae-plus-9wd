package kr.hhplus.be.server.domain.coupon;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    /**
     * 쿠폰을 저장하거나 업데이트한다.
     */
    void save(Coupon coupon);

    /**
     * 쿠폰을 삭제한다.
     */
    Optional<Coupon> findByCode(String code); // 못 찾으면 CouponNotFoundException


    Coupon findByCodeForUpdate(String code);

    Collection<Coupon> findAll();

    Collection<Coupon> findExpiredCoupons();

    boolean wasIssued(long l, String successCase);

    boolean hasIssued(long l, String successCase);
}
