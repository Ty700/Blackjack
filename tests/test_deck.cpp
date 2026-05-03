#include <gtest/gtest.h>
#include <map>
#include <set>
#include <stdexcept>
#include <string>
#include "deck.hpp"

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

// --- Size / construction --------------------------------------------------

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

// Edge case: zero decks falls back to 1 deck (52 cards).
TEST(DeckTest, ZeroDecksFallsBackToSingleDeck) {
    Deck deck(0);
    EXPECT_EQ(drainAndCount(deck), 52u);
}

// Edge case: large but reasonable deck count (uint8_t max-ish).
TEST(DeckTest, LargeDeckCountHasCorrectSize) {
    const uint8_t n = 50;
    Deck deck(n);
    EXPECT_EQ(drainAndCount(deck), static_cast<size_t>(n) * 52u);
}

// --- Empty / draw behavior ------------------------------------------------

TEST(DeckTest, DrawFromEmptyDeckThrows) {
    Deck deck(1);
    drainAndCount(deck);
    EXPECT_THROW(deck.drawCard(), std::runtime_error);
}

TEST(DeckTest, DrawAfterPartialDrainStillWorks) {
    Deck deck(1);
    for (int i = 0; i < 26; ++i) {
        EXPECT_NO_THROW(deck.drawCard());
    }
    EXPECT_NO_THROW(deck.drawCard());
}

TEST(DeckTest, RepeatedThrowAfterEmpty) {
    Deck deck(1);
    drainAndCount(deck);
    EXPECT_THROW(deck.drawCard(), std::runtime_error);
    EXPECT_THROW(deck.drawCard(), std::runtime_error);
}

// --- Card field validity --------------------------------------------------

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
        "Two","Three","Four","Five","Six","Seven","Eight","Nine","Ten",
        "Jack","Queen","King","Ace"
    };
    for (int i = 0; i < 52; ++i) {
        auto card = deck.drawCard();
        EXPECT_TRUE(validNames.count(card.name)) << "Bad name: " << card.name;
    }
}

TEST(DeckTest, AllCardsHaveValidSymbols) {
    Deck deck(1);
    const std::set<std::string> validSymbols = {
        "2","3","4","5","6","7","8","9","10","J","Q","K","A"
    };
    for (int i = 0; i < 52; ++i) {
        auto card = deck.drawCard();
        EXPECT_TRUE(validSymbols.count(card.symbol)) << "Bad symbol: " << card.symbol;
    }
}

TEST(DeckTest, NameSymbolPairingIsConsistent) {
    Deck deck(1);
    const std::map<std::string, std::string> expectedSymbol = {
        {"Two","2"}, {"Three","3"}, {"Four","4"}, {"Five","5"}, {"Six","6"},
        {"Seven","7"}, {"Eight","8"}, {"Nine","9"}, {"Ten","10"},
        {"Jack","J"}, {"Queen","Q"}, {"King","K"}, {"Ace","A"}
    };
    for (int i = 0; i < 52; ++i) {
        auto card = deck.drawCard();
        auto it = expectedSymbol.find(card.name);
        ASSERT_NE(it, expectedSymbol.end()) << "Unknown name: " << card.name;
        EXPECT_EQ(card.symbol, it->second)
            << "Name " << card.name << " has wrong symbol " << card.symbol;
    }
}

TEST(DeckTest, CardValuesMatchBlackjackBaseRules) {
    Deck deck(1);
    for (int i = 0; i < 52; ++i) {
        auto card = deck.drawCard();
        if (card.name == "Jack" || card.name == "Queen" || card.name == "King") {
            EXPECT_EQ(card.value, 10);
        } else if (card.name == "Ace") {
            EXPECT_TRUE(card.value == 1 || card.value == 11)
                << "Ace has invalid value: " << static_cast<int>(card.value);
        } else {
            // Numeric face value lives in the symbol field now.
            int expected = std::stoi(card.symbol);
            EXPECT_EQ(card.value, expected);
        }
    }
}

// --- Composition ----------------------------------------------------------

TEST(DeckTest, SingleDeckContainsExactly13OfEachSuit) {
    Deck deck(1);
    std::map<std::string, int> suitCount;
    for (int i = 0; i < 52; ++i) {
        suitCount[deck.drawCard().suit]++;
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
        nameCount[deck.drawCard().name]++;
    }
    for (const auto& name : {"Two","Three","Four","Five","Six","Seven","Eight",
                             "Nine","Ten","Jack","Queen","King","Ace"}) {
        EXPECT_EQ(nameCount[name], 4) << "Wrong count for " << name;
    }
}

TEST(DeckTest, EverySuitRankPairAppearsExactlyOncePerDeck) {
    Deck deck(1);
    std::map<std::pair<std::string,std::string>, int> seen;
    for (int i = 0; i < 52; ++i) {
        auto c = deck.drawCard();
        seen[{c.suit, c.name}]++;
    }
    EXPECT_EQ(seen.size(), 52u);
    for (const auto& kv : seen) {
        EXPECT_EQ(kv.second, 1) << "Duplicate: " << kv.first.first << "/" << kv.first.second;
    }
}

TEST(DeckTest, MultiDeckHasCorrectMultiplicityPerCard) {
    const uint8_t n = 4;
    Deck deck(n);
    std::map<std::pair<std::string,std::string>, int> seen;
    while (true) {
        try {
            auto c = deck.drawCard();
            seen[{c.suit, c.name}]++;
        } catch (const std::runtime_error&) { break; }
    }
    EXPECT_EQ(seen.size(), 52u);
    for (const auto& kv : seen) {
        EXPECT_EQ(kv.second, n) << kv.first.first << "/" << kv.first.second;
    }
}

// Edge case: total point value of a single deck is fixed regardless of order.
// 4 suits × (2+3+4+5+6+7+8+9+10) = 216
// 4 suits × 3 face cards × 10 = 120
// 4 aces × 11 = 44
// Total = 380
TEST(DeckTest, SingleDeckTotalPointSumIs380) {
    Deck deck(1);
    int total = 0;
    for (int i = 0; i < 52; ++i) {
        total += deck.drawCard().value;
    }
    EXPECT_EQ(total, 380);
}

