#pragma once
#include <cstdint>
#include <string>
#include <stdint.h>
#include <vector>
#include <stack>
#include <stdexcept>

class Deck {
    private:
        struct Card {
            std::string     suit;
            std::string     symbol;
            uint8_t         value;
            std::string     name;
        };
        
        /* Members */
        uint8_t num_decks {6};

        /* Methods */
        std::vector<Card> gameDeckAsVector;
        std::stack<Card, std::vector<Card>> gameDeck;
        void shuffle(const uint8_t& number_of_shuffles = 1);

    public:
        /* Constructor. 
         * num_decks: The number of decks to use in the game. Default is 6, which is common in blackjack games.
         * Creates a deck of cards with the specified number of decks, shuffles it, and prepares it for drawing.
        */
        Deck(const uint8_t& num_decks = 6);

        Card drawCard();
};
