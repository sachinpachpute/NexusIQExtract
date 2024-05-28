package com.vocalink.bacs;

import java.util.HashMap;
import java.util.Map;

public class Configuration {
    public static final Map<String, String> applications = new HashMap<>();

    static {

        applications.put("f7c40f604aa84b99aa787d5cd0db6087", "ets");
        applications.put("9fdcfa10d35a4f109e006a7afe1d3519", "acsw-xval");

        /*applications.put("f7c40f604aa84b99aa787d5cd0db6087", "ets");
        applications.put("9fdcfa10d35a4f109e006a7afe1d3519", "acsw-xval");
        applications.put("221c790043c245e982e29fc83cb1bfad", "acsw-engine");
        applications.put("3d1eafcb67ae4eb18a35c4ddf16f55d6", "acsw-ui");
        applications.put("78a795c3d05044c4ba7297c15bdc7c0b", "psw");
        applications.put("78b741479d47430898a26b8139adb48b", "psw_refresh");
        applications.put("195f723a225547c79dd36e7178c50084", "psw_ocs");
        applications.put("080393c35570400aab3c46c5ec483e24", "busw-engine");
        applications.put("6c89e3aee3de4cd1a86859f3672cfcfc", "meaud");
        applications.put("b42ede4aee324efb8cf9eac1b1d8b612", "meadd");
        applications.put("903e31dafbcb4170a3951771d43740bd", "isa-engine");
        applications.put("182533052da84b7a8f13a4c8a27057a9", "soap-ejb");
        applications.put("b497209b3ee54daabf31d2b0152f0651", "refdata");
        applications.put("8cd83362e4b24968a78429a44b4bcbe9", "security");
        applications.put("463d9191b84842ef8285e113f2d0f41b", "pe");
        applications.put("23d35a66f6fe4956878a4edd151ceda8", "pem");
        applications.put("7c16bb09c1b34022884cd316b44f6be2", "mem");
        applications.put("7f6eb0f90d664530aab59899c10e4d02", "gnf");
        applications.put("2d957c18d4d241db8d186c0139ecfba9", "grs");
        applications.put("46deca35f3194705a1773cab52d7f10f", "bip-core");
        applications.put("919589f6a6ce49f99f9f5d944e82ca5c", "bip-serv");
        applications.put("20856de980674285b785e703c90f1654", "bip-sub");
        applications.put("03aff8b79cf445e28361a7dc69ea91a8", "bip-tran");
        applications.put("4e339c27f4c94b2b8cc4b27114019014", "sar");
        applications.put("9bd41f9a39044567a5f80cfe727b2e7c", "unf");
        applications.put("7113f2ed440e441a953db4a56ee0e73d", "isa-ui");
        applications.put("24b9b5edc5954dc39f7271dfc1c40985", "brts");
        applications.put("d12cda92e475476ba4af4713516eec5c", "nbcrb");
        applications.put("4ef57d36dd27446080373073ac62089e", "dfg");
        applications.put("c7a502df99bf47e795523024c91b9aed", "actlog");
        applications.put("21b2c0a994ea40cc91c720f4c1e499df", "sts");
        applications.put("2b30e6ebd68a4837986929be80910018", "mi");
        applications.put("cc6f76e9e55644dcbf509686d59988de", "mic");*/
        //applications.put("5e829c6689c34d0e8d4f073416745b41", "bacs-all");
        //applications.put("cb8fc10103e146929f625690d7f383e0", "acsw-com");
        //applications.put("ea3ef8f611e740ec9cf51194d714c430", "security-provider__cscommon");
        //applications.put("8f8df98fb060461893f6155c6a4b6e5f", "pki-simulator");
        //applications.put("ee8f5b3faa234accb855c37c6ba833d0", "vocataglibs__cscommon");
        //applications.put("bea0cbf5b8ee49769b5823bef0bb9ef4", "vocastruts__cscommon");
        //applications.put("68173fb1fc6f4bf295d9bccc386d1760", "pem-spi__csbacs");
        //applications.put("0a25263a9c3b43ecaab02e14e18e126a", "bip-fra__csbacs");
        //applications.put("0f940789dfcc4bb080bc407e5c1ede11", "isa-com");
    }

    public static Map<String, String> getApplications(){
        return applications;
    }
}
