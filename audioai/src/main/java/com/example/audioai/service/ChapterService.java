package com.example.audioai.service;

import com.example.audioai.dto.reponse.ChapterDTO;
import com.example.audioai.dto.requests.CreateChapterRequest;
import com.example.audioai.dto.requests.UpdateChapterRequest;
import com.example.audioai.entity.Chapter;
import com.example.audioai.entity.Story;
import com.example.audioai.entity.User;
import com.example.audioai.exception.ApiException;
import com.example.audioai.repository.ChapterRepository;
import com.example.audioai.repository.StoryOwnershipRepository;
import com.example.audioai.repository.StoryRepository;
import com.example.audioai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ quản lý chương truyện (CRUD) dành cho admin.
 */
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final FileStorageService fileStorageService;
    private final StoryOwnershipRepository ownershipRepository;
    private final UserRepository userRepository;

    /**
     * Upload file audio của chương: lưu vào {@code Data/<TênTruyện>/} và trả về
     * đường dẫn {@code /Data/<TênTruyện>/<file>} để gán vào {@code Chapter.audioUrl}.
     *
     * @param file    file audio upload
     * @param storyId ID truyện (dùng để lấy tên truyện -> đặt thư mục)
     */
    public String uploadChapterAudio(MultipartFile file, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện"));
        return fileStorageService.saveStoryAudio(file, story.getTitle());
    }

    @Transactional(readOnly = true)
    public List<ChapterDTO> getAllChapters() {
        return chapterRepository.findAllByOrderByStoryIdAscChapterNumberAsc()
                .stream()
                .map(this::mapToChapterDTO)
                .collect(Collectors.toList());
    }

    /** Lấy số chương (danh sách chương) của một truyện. */
    @Transactional(readOnly = true)
    public List<ChapterDTO> getChaptersByStory(Long storyId) {
        if (!storyRepository.existsById(storyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện");
        }
        return chapterRepository.findByStoryIdOrderByChapterNumberAsc(storyId)
                .stream()
                .map(this::mapToChapterDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách chương của truyện với ownership check.
     * Nếu user đã mua truyện (có record trong story_ownerships), 
     * tất cả chương sẽ được mở khóa (isFree = true).
     */
    @Transactional(readOnly = true)
    public List<ChapterDTO> getChaptersByStory(Long storyId, UserDetails userDetails) {
        if (!storyRepository.existsById(storyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện");
        }

        // Check if user owns this story
        boolean ownsStory = false;
        if (userDetails != null) {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null) {
                ownsStory = ownershipRepository.existsByUserIdAndStoryId(user.getId(), storyId);
            }
        }

        final boolean unlockAll = ownsStory;
        
        return chapterRepository.findByStoryIdOrderByChapterNumberAsc(storyId)
                .stream()
                .map(chapter -> mapToChapterDTO(chapter, unlockAll))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChapterDTO getChapterById(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy chương"));
        return mapToChapterDTO(chapter);
    }

    @Transactional
    public ChapterDTO createChapter(CreateChapterRequest request) {
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện"));

        if (chapterRepository.existsByStoryIdAndChapterNumber(
                story.getId(), request.getChapterNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Số chương " + request.getChapterNumber() + " đã tồn tại trong truyện này");
        }

        Chapter chapter = new Chapter();
        chapter.setStory(story);
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setAudioUrl(request.getAudioUrl());
        chapter.setIsFree(request.getIsFree() != null ? request.getIsFree() : Boolean.FALSE);
        chapter.setCreatedAt(LocalDateTime.now());

        Chapter saved = chapterRepository.save(chapter);
        return mapToChapterDTO(saved);
    }

    @Transactional
    public ChapterDTO updateChapter(Long id, UpdateChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy chương"));

        if (request.getChapterNumber() != null) {
            boolean changed = !request.getChapterNumber().equals(chapter.getChapterNumber());
            if (changed && chapterRepository.existsByStoryIdAndChapterNumber(
                    chapter.getStory().getId(), request.getChapterNumber())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Số chương " + request.getChapterNumber() + " đã tồn tại trong truyện này");
            }
            chapter.setChapterNumber(request.getChapterNumber());
        }
        if (request.getTitle() != null) {
            chapter.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            chapter.setContent(request.getContent());
        }
        if (request.getAudioUrl() != null) {
            chapter.setAudioUrl(request.getAudioUrl());
        }
        if (request.getIsFree() != null) {
            chapter.setIsFree(request.getIsFree());
        }

        Chapter updated = chapterRepository.save(chapter);
        return mapToChapterDTO(updated);
    }

    @Transactional
    public void deleteChapter(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy chương"));
        chapterRepository.delete(chapter);
    }

    private ChapterDTO mapToChapterDTO(Chapter chapter) {
        return mapToChapterDTO(chapter, false);
    }

    private ChapterDTO mapToChapterDTO(Chapter chapter, boolean unlockAll) {
        return ChapterDTO.builder()
                .id(chapter.getId())
                .storyId(chapter.getStory().getId())
                .storyTitle(chapter.getStory().getTitle())
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .content(chapter.getContent())
                .audioUrl(chapter.getAudioUrl())
                .isFree(unlockAll ? true : chapter.getIsFree()) // Override isFree if user owns story
                .createdAt(chapter.getCreatedAt())
                .build();
    }
}