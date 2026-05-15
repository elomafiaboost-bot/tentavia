#pragma once
#include <vector>

// Entidades capturadas via hooks glScalef / glTranslatef durante a renderiza��o do jogo.
// Populado a cada frame e limpo ap�s ESP::Render().
namespace GLCapture {

    struct Entity {
        float mv[16];  // modelview com offset de centro j� aplicado
        float pr[16];  // projection matrix
    };

    extern std::vector<Entity> players;
    extern std::vector<Entity> chests;
    extern std::vector<Entity> largeChests;

    void Clear();
}
