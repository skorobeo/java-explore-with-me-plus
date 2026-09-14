package ru.practicum.ewm.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.comment.model.Comment;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByEventId(Long eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByAuthorId(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Optional<Comment> findByIdAndAuthorId(Long id, Long authorId);

    @EntityGraph(attributePaths = {"author"})
    @Query("select c from Comment c")
    Page<Comment> findAllWithAuthor(Pageable pageable);
}