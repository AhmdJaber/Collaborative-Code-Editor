package com.example.CodeEditor.repository;

import com.example.CodeEditor.model.component.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    @Query(value = """
      select t from Token t inner join Client u\s
      on t.client.id = u.id\s
      where u.id = :id\s
""")

    List<Token> findAllValidTokenClient(Long id);

    Optional<Token> findByToken(String token);
}
