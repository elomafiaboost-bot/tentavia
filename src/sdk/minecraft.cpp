#include "minecraft.hpp"
#include "jni_utils.hpp"
#include <iostream>

namespace SDK {

    namespace Classes {
        const char* Minecraft    = "net/minecraft/client/Minecraft";
        const char* EntityPlayerSP = "net/minecraft/client/entity/EntityPlayerSP";
        const char* WorldClient  = "net/minecraft/client/multiplayer/WorldClient";
    }

    void Minecraft::PrintLocalPlayerName() {
        JNIEnv* env = JNIUtils::GetJNIEnv();
        if (!env) {
            std::cout << "[-] JNIEnv nao disponivel." << std::endl;
            return;
        }
        auto* f = env->functions;

        // 1. Obtém a classe net.minecraft.client.Minecraft
        jclass mcClass = f->FindClass(env, Classes::Minecraft);
        if (!mcClass) {
            f->ExceptionClear(env);
            std::cout << "[-] Classe Minecraft nao encontrada (servidor dedicado?)." << std::endl;
            return;
        }

        // 2. Obtém o método estático getMinecraft()
        jmethodID getMinecraft = f->GetStaticMethodID(
            env, mcClass, "getMinecraft", "()Lnet/minecraft/client/Minecraft;"
        );
        if (!getMinecraft) {
            f->ExceptionClear(env);
            std::cout << "[-] Método getMinecraft() nao encontrado." << std::endl;
            return;
        }

        // 3. Chama Minecraft.getMinecraft() para obter a instância
        jobject mcInstance = f->CallStaticObjectMethod(env, mcClass, getMinecraft);
        if (!mcInstance || f->ExceptionCheck(env)) {
            f->ExceptionClear(env);
            std::cout << "[-] getMinecraft() retornou null." << std::endl;
            return;
        }

        // 4. Acessa o campo thePlayer (EntityPlayerSP)
        jfieldID thePlayerField = f->GetFieldID(
            env, mcClass, "thePlayer", "Lnet/minecraft/client/entity/EntityPlayerSP;"
        );
        if (!thePlayerField) {
            f->ExceptionClear(env);
            std::cout << "[-] Campo thePlayer nao encontrado." << std::endl;
            return;
        }

        jobject player = f->GetObjectField(env, mcInstance, thePlayerField);
        if (!player || f->ExceptionCheck(env)) {
            f->ExceptionClear(env);
            std::cout << "[-] thePlayer eh null (nao em um mundo?)." << std::endl;
            return;
        }

        // 5. Chama player.getName() para obter o nome do jogador
        jclass playerClass = f->GetObjectClass(env, player);
        jmethodID getNameMethod = f->GetMethodID(
            env, playerClass, "getName", "()Ljava/lang/String;"
        );
        if (!getNameMethod) {
            f->ExceptionClear(env);
            std::cout << "[-] Método getName() nao encontrado em EntityPlayer." << std::endl;
            return;
        }

        jstring nameStr = (jstring)f->CallObjectMethod(env, player, getNameMethod);
        if (!nameStr || f->ExceptionCheck(env)) {
            f->ExceptionClear(env);
            std::cout << "[-] getName() retornou null." << std::endl;
            return;
        }

        // 6. Converte jstring para char* e exibe
        const char* name = f->GetStringUTFChars(env, nameStr, nullptr);
        if (name) {
            std::cout << "[SDK] LocalPlayer: " << name << std::endl;
            f->ReleaseStringUTFChars(env, nameStr, name);
        }
    }
}
