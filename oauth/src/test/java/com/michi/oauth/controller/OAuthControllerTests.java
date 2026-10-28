package com.michi.oauth.controller;

import com.michi.oauth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuthControllerTests {

    private UserService userService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        mvc = MockMvcBuilders.standaloneSetup(new OAuthController(userService)).build();
    }

    @Test
    void markFavoriteWithoutUserIdIsForbidden() throws Exception {
        mvc.perform(patch("/auth/user/abc").param("productId", "p1"))
                .andExpect(status().isForbidden());
        verify(userService, never()).markFavorite("abc", "p1");
    }

    @Test
    void markFavoriteOfAnotherUserIsForbidden() throws Exception {
        mvc.perform(patch("/auth/user/abc").param("productId", "p1").header("User-Id", "other"))
                .andExpect(status().isForbidden());
        verify(userService, never()).markFavorite("abc", "p1");
    }

    @Test
    void unmarkFavoriteOfAnotherUserIsForbidden() throws Exception {
        mvc.perform(delete("/auth/user/abc").param("productId", "p1").header("User-Id", "other"))
                .andExpect(status().isForbidden());
        verify(userService, never()).unmarkFavorite("abc", "p1");
    }

    @Test
    void markFavoriteOfOwnUserWorks() throws Exception {
        mvc.perform(patch("/auth/user/abc").param("productId", "p1").header("User-Id", "abc"))
                .andExpect(status().isOk());
        verify(userService).markFavorite("abc", "p1");
    }

    @Test
    void userWithoutHeadersIsGuestAndSkipsDatabase() throws Exception {
        mvc.perform(get("/auth/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Invitado"));
        verify(userService, never()).favoriteProducts("");
    }
}
