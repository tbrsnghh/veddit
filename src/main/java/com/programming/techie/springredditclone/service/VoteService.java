package com.programming.techie.springredditclone.service;

import com.programming.techie.springredditclone.dto.VoteDto;
import com.programming.techie.springredditclone.exceptions.PostNotFoundException;
import com.programming.techie.springredditclone.exceptions.SpringRedditException;
import com.programming.techie.springredditclone.model.Post;
import com.programming.techie.springredditclone.model.Vote;
import com.programming.techie.springredditclone.repository.PostRepository;
import com.programming.techie.springredditclone.repository.VoteRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.programming.techie.springredditclone.model.VoteType.UPVOTE;

@Service
@AllArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final AuthService authService;

    @Transactional
    public void vote(VoteDto voteDto) {
        Post post = postRepository.findById(voteDto.getPostId())
                .orElseThrow(() -> new PostNotFoundException("Post Not Found with ID - " + voteDto.getPostId()));

        Optional<Vote> voteByPostAndUser = voteRepository.findTopByPostAndUserOrderByVoteIdDesc(post, authService.getCurrentUser());

        // If there is a previous vote from the user
        if (voteByPostAndUser.isPresent()) {
            Vote previousVote = voteByPostAndUser.get();

            // Case 1: User clicks the same vote type again (remove vote)
            if (previousVote.getVoteType().equals(voteDto.getVoteType())) {
                // Reset the vote count to original by reversing the previous vote
                if (UPVOTE.equals(previousVote.getVoteType())) {
                    post.setVoteCount(post.getVoteCount() - 1); // Remove the previous upvote
                } else {
                    post.setVoteCount(post.getVoteCount() + 1); // Remove the previous downvote
                }

                // Delete the previous vote, as the user is canceling their vote
                voteRepository.delete(previousVote);
            }
            // Case 2: User toggles to the opposite vote type (e.g., upvote to downvote)
            else {
                // Toggle the vote count by ±2
                if (UPVOTE.equals(voteDto.getVoteType())) {
                    post.setVoteCount(post.getVoteCount() + 2); // Changing from downvote to upvote
                } else {
                    post.setVoteCount(post.getVoteCount() - 2); // Changing from upvote to downvote
                }

                // Update the previous vote to the new vote type
                previousVote.setVoteType(voteDto.getVoteType());
                voteRepository.save(previousVote);
            }
        }
        // No previous vote, so simply apply the current vote type
        else {
            if (UPVOTE.equals(voteDto.getVoteType())) {
                post.setVoteCount(post.getVoteCount() + 1);
            } else {
                post.setVoteCount(post.getVoteCount() - 1);
            }

            // Save the new vote
            voteRepository.save(mapToVote(voteDto, post));
        }

        // Save the post with the updated vote count
        postRepository.save(post);
    }



    private Vote mapToVote(VoteDto voteDto, Post post) {
        return Vote.builder()
                .voteType(voteDto.getVoteType())
                .post(post)
                .user(authService.getCurrentUser())
                .build();
    }
}
