#pragma once
#include <cstdint>
#include <memory>
#include <stdlib.h> 
#include <string>
#include <stdint.h>
#include <array>
#include <vector>
#include <stack>
#include <unordered_map>
#include <stdexcept>

class Card {
    /* Todo b/c of the way ace is handled, we need to make the value of the card dynamic. */
    /* See the construction of the deck for why.. */
    /* TODO */
};

class Deck {
    private:
        struct Card {
            std::string     suit;
            uint8_t         value;
            std::string     name;
        };
        std::vector<Card> gameDeckAsVector;
        std::stack<Card, std::vector<Card>> gameDeck;

    public:
        /* Constructor. 
         * num_decks: The number of decks to use in the game. Default is 6, which is common in blackjack games.
         * Creates a deck of cards with the specified number of decks, shuffles it, and prepares it for drawing.
        */
        Deck(const uint8_t& num_decks = 6) {
            const std::array<std::string, 4> suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
            /* TODO Absstract deck constructino to a separate function. In addition, construct card class */
            const std::unordered_map<uint8_t, std::array<std::string, 2>> cardNames = 
            {
                /* Maps a search idx to a card's symbol and value */
                {0, {"Two",     "2"}},
                {1, {"Three",   "3"}},
                {2, {"Four",    "4"}},
                {3, {"Five",    "5"}},
                {4, {"Six",     "6"}},
                {5, {"Seven",   "7"}},
                {6, {"Eight",   "8"}},
                {7, {"Nine",    "9"}},
                {8, {"Ten",    "10"}},
                {9, {"Jack",   "10"}},
                {10, {"Queen",  "10"}},
                {11, {"King",   "10"}},
                {12, {"Ace",    "11"}}
            };
            
            /* Builds the deck */
            for (uint8_t i = 0; i < num_decks; i++) {
                for (const auto& suit : suits) {
                    for (uint8_t j = 0; j < 13; j++) {
                        Card card = {.suit = suit, .value = cardNames.find(j+2), .name = };
                        gameDeckAsVector.push_back(card);
                    }
                }
            }
            shuffle(4);
            /* Makes the stack from the vector. */
            for (const auto& card : gameDeckAsVector) {
                gameDeck.push(card);
            }
        }

        void shuffle(const uint8_t& number_of_shuffles = 1) {
            /* Shuffles the deck using the Fisher-Yates algorithm. */
            for(uint8_t i = 0; i < number_of_shuffles; i++) {
                for (size_t i = gameDeckAsVector.size() - 1; i > 0; i--) {
                    size_t j = rand() % (i + 1);
                    std::swap(gameDeckAsVector[i], gameDeckAsVector[j]);
                }
            }
        }

        Card drawCard() {
            /* Draws a card from the top of the deck. */ 
            if (gameDeck.empty())
            {
                throw std::runtime_error("Deck is empty");
            }
            
            Card card = gameDeck.top();
            gameDeck.pop();
            return card;
        }
};
