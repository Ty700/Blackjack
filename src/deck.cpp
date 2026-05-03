#include "deck.hpp"
#include <array>
#include <random>

Deck::Deck(const uint8_t& number_of_decks) {
    /* Default to 1 deck if an invalid number is provided */
    num_decks = (number_of_decks < 1) ? 1 : number_of_decks;

    const std::array<std::string, 4> suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
    const std::array<std::pair<std::string, std::pair<std::string, uint8_t>>, 13> cardNames =
    /* Maps a name of card to symbol and value pair */
    {
        {       
        {"Two",     {"2", 2}},
        {"Three",   {"3", 3}},
        {"Four",    {"4", 4}},
        {"Five",    {"5", 5}},
        {"Six",     {"6", 6}},
        {"Seven",   {"7", 7}},
        {"Eight",   {"8", 8}},
        {"Nine",    {"9", 9}},
        {"Ten",     {"10", 10}},
        {"Jack",    {"J", 10}},
        {"Queen",   {"Q", 10}},
        {"King",    {"K", 10}},
        {"Ace",     {"A", 11}}
        }
    };
    
    /* Builds the deck */
    for (uint8_t i = 0; i < num_decks; i++) {
        for (const auto& suit : suits) {
            for (uint8_t j = 0; j < 13; j++) {
                Card card = {
                    .suit = suit,
                    .symbol = cardNames[j].second.first,
                    .value = cardNames[j].second.second,
                    .name = cardNames[j].first
                };
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

void Deck::shuffle(const uint8_t& number_of_shuffles){
    /* Shuffles the deck using the Fisher-Yates algorithm. */
    for(uint8_t i = 0; i < number_of_shuffles; i++) {
        for (size_t i = gameDeckAsVector.size() - 1; i > 0; i--) {
            std::random_device rd;
            std::mt19937 gen(rd());
            std::uniform_int_distribution<> dis(0, i);
            size_t j = dis(gen);
            std::swap(gameDeckAsVector[i], gameDeckAsVector[j]);
        }
    }
}

Deck::Card Deck::drawCard() {
    /* Draws a card from the top of the deck. */ 
    if (gameDeck.empty())
    {
        throw std::runtime_error("Deck is empty");
    }

    Card card = gameDeck.top();
    gameDeck.pop();
    return card;
}


