#include "minecraft.hpp"
#include "jni_utils.hpp"
#include <iostream>

namespace SDK {

namespace Classes {
    const char* Minecraft    = "net/minecraft/client/Minecraft";
    const char* EntityPlayerSP = "net/minecraft/client/entity/EntityPlayerSP";
    const char* WorldClient  = "net/minecraft/client/multiplayer/WorldClient";
}

// ── helpers internos ─────────────────────────────────────────────────────────

static JNIEnv* Env() { return JNIUtils::GetJNIEnv(); }

// Retorna a instância Minecraft.getMinecraft(), ou nullptr.
static jobject GetMCInstance(JNIEnv* env) {
    auto* f = env->functions;
    jclass cls = f->FindClass(env, Classes::Minecraft);
    if (!cls) { f->ExceptionClear(env); return nullptr; }
    jmethodID m = f->GetStaticMethodID(env, cls, "getMinecraft",
                                       "()Lnet/minecraft/client/Minecraft;");
    if (!m) { f->ExceptionClear(env); return nullptr; }
    jobject mc = f->CallStaticObjectMethod(env, cls, m);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return mc;
}

// ── PrintLocalPlayerName ─────────────────────────────────────────────────────

void Minecraft::PrintLocalPlayerName() {
    JNIEnv* env = Env();
    if (!env) { std::cout << "[-] JNIEnv nao disponivel." << std::endl; return; }
    auto* f = env->functions;

    jclass mcClass = f->FindClass(env, Classes::Minecraft);
    if (!mcClass) { f->ExceptionClear(env);
        std::cout << "[-] Classe Minecraft nao encontrada." << std::endl; return; }

    jmethodID getMinecraft = f->GetStaticMethodID(env, mcClass, "getMinecraft",
                                                  "()Lnet/minecraft/client/Minecraft;");
    if (!getMinecraft) { f->ExceptionClear(env);
        std::cout << "[-] Metodo getMinecraft() nao encontrado." << std::endl; return; }

    jobject mc = f->CallStaticObjectMethod(env, mcClass, getMinecraft);
    if (!mc || f->ExceptionCheck(env)) { f->ExceptionClear(env);
        std::cout << "[-] getMinecraft() retornou null." << std::endl; return; }

    jfieldID thePlayerField = f->GetFieldID(env, mcClass, "thePlayer",
                                            "Lnet/minecraft/client/entity/EntityPlayerSP;");
    if (!thePlayerField) { f->ExceptionClear(env);
        std::cout << "[-] Campo thePlayer nao encontrado." << std::endl; return; }

    jobject player = f->GetObjectField(env, mc, thePlayerField);
    if (!player || f->ExceptionCheck(env)) { f->ExceptionClear(env);
        std::cout << "[-] thePlayer eh null." << std::endl; return; }

    jclass playerClass = f->GetObjectClass(env, player);
    jmethodID getName = f->GetMethodID(env, playerClass, "getName", "()Ljava/lang/String;");
    if (!getName) { f->ExceptionClear(env);
        std::cout << "[-] Metodo getName() nao encontrado." << std::endl; return; }

    jstring nameStr = (jstring)f->CallObjectMethod(env, player, getName);
    if (!nameStr || f->ExceptionCheck(env)) { f->ExceptionClear(env); return; }

    const char* name = f->GetStringUTFChars(env, nameStr, nullptr);
    if (name) {
        std::cout << "[SDK] LocalPlayer: " << name << std::endl;
        f->ReleaseStringUTFChars(env, nameStr, name);
    }
}

// ── GetCameraInfo ─────────────────────────────────────────────────────────────

bool Minecraft::GetCameraInfo(CameraInfo& out) {
    JNIEnv* env = Env();
    if (!env) return false;
    auto* f = env->functions;

    jclass mcClass = f->FindClass(env, Classes::Minecraft);
    if (!mcClass) { f->ExceptionClear(env); return false; }

    jmethodID getMinecraft = f->GetStaticMethodID(env, mcClass, "getMinecraft",
                                                  "()Lnet/minecraft/client/Minecraft;");
    if (!getMinecraft) { f->ExceptionClear(env); return false; }

    jobject mc = f->CallStaticObjectMethod(env, mcClass, getMinecraft);
    if (!mc || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jfieldID thePlayerF = f->GetFieldID(env, mcClass, "thePlayer",
                                        "Lnet/minecraft/client/entity/EntityPlayerSP;");
    if (!thePlayerF) { f->ExceptionClear(env); return false; }

    jobject player = f->GetObjectField(env, mc, thePlayerF);
    if (!player || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jclass playerClass = f->GetObjectClass(env, player);

    jfieldID pxF    = f->GetFieldID(env, playerClass, "posX",          "D");
    jfieldID pyF    = f->GetFieldID(env, playerClass, "posY",          "D");
    jfieldID pzF    = f->GetFieldID(env, playerClass, "posZ",          "D");
    jfieldID yawF   = f->GetFieldID(env, playerClass, "rotationYaw",   "F");
    jfieldID pitchF = f->GetFieldID(env, playerClass, "rotationPitch", "F");

    if (!pxF || !pyF || !pzF || !yawF || !pitchF) {
        f->ExceptionClear(env); return false;
    }

    out.eyeX  = f->GetDoubleField(env, player, pxF);
    out.eyeY  = f->GetDoubleField(env, player, pyF) + 1.62; // eye height
    out.eyeZ  = f->GetDoubleField(env, player, pzF);
    out.yaw   = f->GetFloatField(env, player, yawF);
    out.pitch = f->GetFloatField(env, player, pitchF);
    out.fov   = 70.0f; // fallback

    // Tenta pegar fovSetting de GameSettings
    jfieldID gsF = f->GetFieldID(env, mcClass, "gameSettings",
                                 "Lnet/minecraft/client/settings/GameSettings;");
    if (gsF) {
        jobject gs = f->GetObjectField(env, mc, gsF);
        if (gs) {
            jclass gsClass = f->GetObjectClass(env, gs);
            jfieldID fovF  = f->GetFieldID(env, gsClass, "fovSetting", "F");
            if (fovF) out.fov = f->GetFloatField(env, gs, fovF);
            else      f->ExceptionClear(env);
            f->DeleteLocalRef(env, gs);
        }
    } else {
        f->ExceptionClear(env);
    }

    out.valid = true;
    return true;
}

// ── GetNearbyPlayers ──────────────────────────────────────────────────────────

bool Minecraft::GetNearbyPlayers(std::vector<EntityInfo>& out) {
    JNIEnv* env = Env();
    if (!env) return false;
    auto* f = env->functions;

    jclass mcClass = f->FindClass(env, Classes::Minecraft);
    if (!mcClass) { f->ExceptionClear(env); return false; }

    jmethodID getMinecraft = f->GetStaticMethodID(env, mcClass, "getMinecraft",
                                                  "()Lnet/minecraft/client/Minecraft;");
    if (!getMinecraft) { f->ExceptionClear(env); return false; }

    jobject mc = f->CallStaticObjectMethod(env, mcClass, getMinecraft);
    if (!mc || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    // Local player (para excluir da lista)
    jfieldID thePlayerF = f->GetFieldID(env, mcClass, "thePlayer",
                                        "Lnet/minecraft/client/entity/EntityPlayerSP;");
    jobject localPlayer = nullptr;
    if (thePlayerF) localPlayer = f->GetObjectField(env, mc, thePlayerF);
    if (f->ExceptionCheck(env)) f->ExceptionClear(env);

    // theWorld
    jfieldID theWorldF = f->GetFieldID(env, mcClass, "theWorld",
                                       "Lnet/minecraft/client/multiplayer/WorldClient;");
    if (!theWorldF) { f->ExceptionClear(env); return false; }

    jobject world = f->GetObjectField(env, mc, theWorldF);
    if (!world || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    // world.playerEntities (campo herdado de World)
    jclass worldClass = f->GetObjectClass(env, world);
    jfieldID listF    = f->GetFieldID(env, worldClass, "playerEntities", "Ljava/util/List;");
    if (!listF) { f->ExceptionClear(env); return false; }

    jobject list = f->GetObjectField(env, world, listF);
    if (!list || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jclass listClass   = f->GetObjectClass(env, list);
    jmethodID sizeM    = f->GetMethodID(env, listClass, "size", "()I");
    jmethodID getM     = f->GetMethodID(env, listClass, "get",  "(I)Ljava/lang/Object;");
    if (!sizeM || !getM) { f->ExceptionClear(env); return false; }

    jint count = f->CallIntMethod(env, list, sizeM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    out.reserve((size_t)count);

    for (jint i = 0; i < count; i++) {
        jobject entity = f->CallObjectMethod(env, list, getM, i);
        if (!entity || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

        // Pula o jogador local
        if (localPlayer && f->IsSameObject(env, entity, localPlayer)) {
            f->DeleteLocalRef(env, entity);
            continue;
        }

        jclass entClass = f->GetObjectClass(env, entity);

        jfieldID pxF = f->GetFieldID(env, entClass, "posX", "D");
        jfieldID pyF = f->GetFieldID(env, entClass, "posY", "D");
        jfieldID pzF = f->GetFieldID(env, entClass, "posZ", "D");

        if (!pxF || !pyF || !pzF) {
            f->ExceptionClear(env);
            f->DeleteLocalRef(env, entity);
            continue;
        }

        EntityInfo info;
        info.posX = f->GetDoubleField(env, entity, pxF);
        info.posY = f->GetDoubleField(env, entity, pyF);
        info.posZ = f->GetDoubleField(env, entity, pzF);

        jmethodID nameM = f->GetMethodID(env, entClass, "getName", "()Ljava/lang/String;");
        if (nameM) {
            jstring ns = (jstring)f->CallObjectMethod(env, entity, nameM);
            if (ns && !f->ExceptionCheck(env)) {
                const char* nc = f->GetStringUTFChars(env, ns, nullptr);
                if (nc) { info.name = nc; f->ReleaseStringUTFChars(env, ns, nc); }
                f->DeleteLocalRef(env, ns);
            } else {
                f->ExceptionClear(env);
            }
        }

        out.push_back(info);
        f->DeleteLocalRef(env, entity);
    }

    return true;
}

} // namespace SDK
