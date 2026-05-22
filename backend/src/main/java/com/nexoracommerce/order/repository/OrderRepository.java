package com.nexoracommerce.order.repository;
import com.nexoracommerce.common.repository.BaseRepository;

import com.nexoracommerce.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface OrderRepository extends BaseRepository<Order, String> {
    List<Order> findByUser_Id(UUID userId);
    List<Order> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    @Query("select distinct o from Order o left join fetch o.orderItems")
    List<Order> findAllWithItems();

    @Query("select distinct o from Order o left join fetch o.orderItems where o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") String orderId);

    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems
            where o.user.id = :userId
            order by o.createdAt desc
            """)
    List<Order> findByUserIdOrderByCreatedAtDescWithItems(@Param("userId") UUID userId);

    /**
     * Find all orders with pagination (for admin)
     */
    @Query(value = "select o from Order o order by o.createdAt desc",
           countQuery = "select count(o) from Order o")
    Page<Order> findAllWithItemsPageable(Pageable pageable);

    /**
     * Find orders by user ID with pagination
     */
    @Query(value = """
            select o
            from Order o
            where o.user.id = :userId
            order by o.createdAt desc
            """,
           countQuery = "select count(o) from Order o where o.user.id = :userId")
    Page<Order> findByUserIdWithItemsPageable(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find orders by status with pagination
     */
    Page<Order> findByStatus(String status, Pageable pageable);

    /**
     * Find orders by user ID and status with pagination
     */
    Page<Order> findByUser_IdAndStatus(UUID userId, String status, Pageable pageable);
}
