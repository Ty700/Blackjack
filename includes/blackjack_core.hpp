#pragma once
#include <iostream>

class BlackjackCore {
    public: 
        BlackjackCore() 
        {
            #ifdef DEBUG 
                std::cout << "BlackjackCore constructor called" << std::endl;
            #endif /* DEBUG */
        }
};
