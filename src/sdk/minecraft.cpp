#include "minecraft.hpp"
#include "jni_utils.hpp"
#include <iostream>

namespace SDK {

namespace Classes {
    const char* Minecraft      = "net/minecraft/client/Minecraft";
    const char* EntityPlayerSP = "net/minecraft/client/entity/EntityPlayerSP";
    const char* WorldClient    = "net/minecraft/client/multiplayer/WorldClient";
}

// ── Cache de classes (global refs, vivos enquanto a DLL estiver carregada) ────

static jclass  g_mcClass     = nullptr;
static jclass  g_worldClass  = nullptr;  // classe do objeto world em runtime
static bool    g_initDone    = false;
static bool    g_initFailed  = false;

static JNIEnv* Env() { return JNIUtils::GetJNIEnv(); }

// Resolve e guarda g_mcClass como global ref. Chamado uma vez.
static bool InitClasses(JNIEnv* env) {
    if (g_initDone)  return !g_initFailed;
    g_initDone = true;

    jclass local = JNIUtils::FindMinecraftClass(env, Classes::Minecraft);
    if (!local) {
        std::cout << "[-] SDK: FindMinecraftClass(Minecraft) falhou. "
                     "Classloader nao tem acesso ainda?" << std::endl;
        g_initFailed = true;
        return false;
    }

    g_mcClass = (jclass)env->functions->NewGlobalRef(env, local);
    env->functions->DeleteLocalRef(env, local);
    std::cout << "[+] SDK: classe Minecraft encontrada e cacheada." << std::endl;
    return true;
}

// Retorna a instancia Minecraft.getMinecraft() ou nullptr.
static jobject GetMCInstance(JNIEnv* env) {
    if (!g_mcClass) return nullptr;
    auto* f = env->functions;
    jmethodID m = f->GetStaticMethodID(env, g_mcClass, "getMinecraft",
                                       "()Lnet/minecraft/client/Minecraft;");
    if (!m) { f->ExceptionClear(env); return nullptr; }
    jobject mc = f->CallStaticObjectMethod(env, g_mcClass, m);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return mc;
}

// ── PrintLocalPlayerName ──────────────────────────────────────────────────────

void Minecraft::PrintLocalPlayerName() {
    JNIEnv* env = Env();
    if (!env) { std::cout << "[-] JNIEnv nao disponivel." << std::endl; return; }
    if (!InitClasses(env)) return;

    auto* f = env->functions;

    jobject mc = GetMCInstance(env);
    if (!mc) { std::cout << "[-] getMinecraft() retornou null." << std::endl; return; }

    jfieldID thePlayerF = f->GetFieldID(env, g_mcClass, "thePlayer",
                                        "Lnet/minecraft/client/entity/EntityPlayerSP;");
    if (!thePlayerF) { f->ExceptionClear(env);
        std::cout << "[-] Campo thePlayer nao encontrado." << std::endl; return; }

    jobject player = f->GetObjectField(env, mc, thePlayerF);
    if (!player || f->ExceptionCheck(env)) { f->ExceptionClear(env);
        std::cout << "[-] thePlayer eh null." << std::endl; return; }

    jclass playerClass = f->GetObjectClass(env, player);
    jmethodID getName = f->GetMethodID(env, playerClass, "getName", "()Ljava/lang/String;");
    if (!getName) { f->ExceptionClear(env); return; }

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

    // Tenta inicializar; se ainda nao conseguiu, tenta de novo cada frame
    // (LWJGL pode ter carregado o classloader so agora)
    if (g_initFailed) {
        g_initDone   = false;
        g_initFailed = false;
    }
    if (!InitClasses(env)) return false;

    auto* f = env->functions;

    jobject mc = GetMCInstance(env);
    if (!mc || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jfieldID thePlayerF = f->GetFieldID(env, g_mcClass, "thePlayer",
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
        f->ExceptionClear(env);
        static bool logged = false;
        if (!logged) {
            std::cout << "[-] ESP: campos posX/rotationYaw nao encontrados "
                         "(cliente obfuscado sem Forge?)" << std::endl;
            logged = true;
        }
        return false;
    }

    out.eyeX  = f->GetDoubleField(env, player, pxF);
    out.eyeY  = f->GetDoubleField(env, player, pyF) + 1.62;
    out.eyeZ  = f->GetDoubleField(env, player, pzF);
    out.yaw   = f->GetFloatField(env, player, yawF);
    out.pitch = f->GetFloatField(env, player, pitchF);
    out.fov   = 70.0f;

    jfieldID gsF = f->GetFieldID(env, g_mcClass, "gameSettings",
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
    if (!env || !g_mcClass) return false;
    auto* f = env->functions;

    jobject mc = GetMCInstance(env);
    if (!mc || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jfieldID thePlayerF = f->GetFieldID(env, g_mcClass, "thePlayer",
                                        "Lnet/minecraft/client/entity/EntityPlayerSP;");
    jobject localPlayer = nullptr;
    if (thePlayerF) localPlayer = f->GetObjectField(env, mc, thePlayerF);
    if (f->ExceptionCheck(env)) f->ExceptionClear(env);

    jfieldID theWorldF = f->GetFieldID(env, g_mcClass, "theWorld",
                                       "Lnet/minecraft/client/multiplayer/WorldClient;");
    if (!theWorldF) {
        f->ExceptionClear(env);
        static bool logged = false;
        if (!logged) { std::cout << "[-] ESP: campo theWorld nao encontrado" << std::endl; logged=true; }
        return false;
    }

    jobject world = f->GetObjectField(env, mc, theWorldF);
    if (!world || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jclass worldClass = f->GetObjectClass(env, world);
    jfieldID listF    = f->GetFieldID(env, worldClass, "playerEntities", "Ljava/util/List;");
    if (!listF) {
        f->ExceptionClear(env);
        static bool logged = false;
        if (!logged) { std::cout << "[-] ESP: campo playerEntities nao encontrado" << std::endl; logged=true; }
        return false;
    }

    jobject list = f->GetObjectField(env, world, listF);
    if (!list || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jclass listClass = f->GetObjectClass(env, list);
    jmethodID sizeM  = f->GetMethodID(env, listClass, "size", "()I");
    jmethodID getM   = f->GetMethodID(env, listClass, "get",  "(I)Ljava/lang/Object;");
    if (!sizeM || !getM) { f->ExceptionClear(env); return false; }

    jint count = f->CallIntMethod(env, list, sizeM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    out.reserve((size_t)count);

    for (jint i = 0; i < count; i++) {
        jobject entity = f->CallObjectMethod(env, list, getM, i);
        if (!entity || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

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
