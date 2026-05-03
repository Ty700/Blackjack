#include <gtest/gtest.h>
#include <set>
#include <stdexcept>
#include "cards.hpp"

namespace {

size_t drainAndCount(Deck& deck) {
    size_t count = 0;
    try {
        while (true) {
            deck.drawCard();
            ++count;
        }
    } catch (const std::runtime_error&) {
    }
    return count;
}

}

TEST(DeckTest, DefaultDeckHas312Cards) {
    Deck deck;
    EXPECT_EQ(drainAndCount(deck), 6u * 52u);
}

TEST(DeckTest, SingleDeckHas52Cards) {
    Deck deck(1);
    EXPECT_EQ(drainAndCount(deck), 52u);
}

TEST(DeckTest, ThreeDecksHas156Cards) {
    Deck deck(3);
    EXPECT_EQ(drainAndCount(deck), 3u * 52u);
}

TEST(DeckTest, DrawFromEmptyDeckThrows) {
    Deck deck(1);
    drainAndCount(deck);
    EXPECT_THROW(deck.drawCard(), std::runtime_error);
}

TEST(DeckTest, AllCardsHaveValidSuits) {
    Deck deck(1);
    const std::set<std::string> validSuits = {"Hearts", "Diamonds", "Clubs", "Spades"};
    for (int i = 0; i < 52; ++i) {
        auto card = deck.drawCard();
        EXPECT_TRUE(validSuits.count(card.suit)) << "Bad suit: " << card.suit;
    }
}

TEST(DeckTest, AllCardsHaveValidNames) {
    Deck deck(1);
    const std::set<std::string> validNames = {
        "2","3","4","5","6","7","8","9","10","Jack","Queen","King","Ace"
    };
    for (int i = 0; i < 52; ++i) {
        auto card = deck.drawCard();
        EXPECT_TRUE(validNames.count(card.name)) << "Bad name: " << card.name;
    }
}

TEST(DeckTest, CardValuesMatchBlackjackBaseRules) {
    Deck deck(1);
    for (int i = 0; i < 52; ++i) {
        auto card = deck.drawCard();
        if (card.name == "Jack" || card.name == "Queen" ||
            card.name == "King" || card.name == "Ace") {
            EXPECT_EQ(card.value, 10);
        } else {
            int expected = std::stoi(card.name);
            EXPECT_EQ(card.value, expected);
        }
    }
}

TEST(DeckTest, SingleDeckContainsExactly13OfEachSuit) {
    Deck deck(1);
    std::map<std::string, int> suitCount;
    for (int i = 0; i < 52; ++i) {
        auto card = deck.drawCard();
        suitCount[card.suit]++;
    }
    EXPECT_EQ(suitCount["Hearts"], 13);
    EXPECT_EQ(suitCount["Diamonds"], 13);
    EXPECT_EQ(suitCount["Clubs"], 13);
    EXPECT_EQ(suitCount["Spades"], 13);
}

TEST(DeckTest, SingleDeckContainsExactly4OfEachRank) {
    Deck deck(1);
    std::map<std::string, int> nameCount;
    for (int i = 0; i < 52; ++i) {
        auto card = deck.drawCard();
        nameCount[card.name]++;
    }
    for (const auto& name : {"2","3","4","5","6","7","8","9","10",
                             "Jack","Queen","King","Ace"}) {
        EXPECT_EQ(nameCount[name], 4) << "Wrong count for " << name;
    }
}
