package com.sparta.clonecoding_8be.model;

import com.sparta.clonecoding_8be.dto.EditCommentRequestDto;
import com.sparta.clonecoding_8be.util.Timestamped;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@NoArgsConstructor
@Getter
@Entity
public class Comment extends Timestamped {

    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private Long id;

    @Column(nullable = false)
    private String comment;

    @ManyToOne
    @JoinColumn(name="MEMBER_ID")
    private Member member;

    @ManyToOne
    @JoinColumn(name="POST_ID")
    private Post post;

    @Builder
    public Comment(String comment, Post post, Member member) {
        this.comment = comment;
    }

    public void registCommentInfo(Post post, Member member){
        this.post = post;
        this.member = member;
    }

    public void updateComment(EditCommentRequestDto requestDto) {
        this.comment = requestDto.getComment();
    }
}
