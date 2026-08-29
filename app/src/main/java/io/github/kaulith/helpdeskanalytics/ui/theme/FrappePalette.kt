package io.github.kaulith.helpdeskanalytics.ui.theme

import androidx.compose.ui.graphics.Color

// Raw Frappe color palette. Source of truth: frappe-ui/tailwind/colors.json.
// Do not consume directly in screens; use FrappeSemantic.* instead.
object FrappePalette {

    object Light {
        object Gray {
            val s50 = Color(0xFFF8F8F8); val s100 = Color(0xFFF3F3F3); val s200 = Color(0xFFEDEDED)
            val s300 = Color(0xFFE2E2E2); val s400 = Color(0xFFC7C7C7); val s500 = Color(0xFF999999)
            val s600 = Color(0xFF7C7C7C); val s700 = Color(0xFF525252); val s800 = Color(0xFF383838)
            val s900 = Color(0xFF171717)
        }
        object Blue {
            val s50 = Color(0xFFF2F9FF); val s100 = Color(0xFFE6F4FF); val s200 = Color(0xFFC8E6FF)
            val s300 = Color(0xFFA7D7FD); val s400 = Color(0xFF73BBF6); val s500 = Color(0xFF0289F7)
            val s600 = Color(0xFF007BE0); val s700 = Color(0xFF0070CC); val s800 = Color(0xFF005CA3)
            val s900 = Color(0xFF004880)
        }
        object Green {
            val s50 = Color(0xFFF2FDF4); val s100 = Color(0xFFE4FAEB); val s200 = Color(0xFFC3F9D3)
            val s300 = Color(0xFFA6EFC0); val s400 = Color(0xFF86E0A8); val s500 = Color(0xFF46B37E)
            val s600 = Color(0xFF278F5E); val s700 = Color(0xFF137949); val s800 = Color(0xFF075E35)
            val s900 = Color(0xFF173B2C)
        }
        object Red {
            val s50 = Color(0xFFFFF7F7); val s100 = Color(0xFFFFE7E7); val s200 = Color(0xFFFFD8D8)
            val s300 = Color(0xFFFDC2C2); val s400 = Color(0xFFF79596); val s500 = Color(0xFFE03636)
            val s600 = Color(0xFFCC2929); val s700 = Color(0xFFB52A2A); val s800 = Color(0xFF941F1F)
            val s900 = Color(0xFF6B1515)
        }
        object Amber {
            val s50 = Color(0xFFFDFAED); val s100 = Color(0xFFFFF7D3); val s200 = Color(0xFFFEEDA9)
            val s300 = Color(0xFFFBDB73); val s400 = Color(0xFFFBCC55); val s500 = Color(0xFFE79913)
            val s600 = Color(0xFFDB7706); val s700 = Color(0xFFB35309); val s800 = Color(0xFF91400D)
            val s900 = Color(0xFF763813)
        }
        object Orange {
            val s50 = Color(0xFFFFF9F5); val s100 = Color(0xFFFFEFE4); val s200 = Color(0xFFFFDEC5)
            val s300 = Color(0xFFFFCBA3); val s400 = Color(0xFFF4B07F); val s500 = Color(0xFFE86C13)
            val s600 = Color(0xFFD45A08); val s700 = Color(0xFFBD3E0C); val s800 = Color(0xFF9E3513)
            val s900 = Color(0xFF6B2711)
        }
        object Yellow {
            val s50 = Color(0xFFFFFCEF); val s100 = Color(0xFFFFF7D3); val s200 = Color(0xFFF7E9A8)
            val s300 = Color(0xFFF5E171); val s400 = Color(0xFFF2D14B); val s500 = Color(0xFFEDBA13)
            val s600 = Color(0xFFD1930D); val s700 = Color(0xFFAB6E05); val s800 = Color(0xFF8C5600)
            val s900 = Color(0xFF733F12)
        }
        object Teal {
            val s50 = Color(0xFFF0FDFA); val s100 = Color(0xFFE6F7F4); val s200 = Color(0xFFBAE8E1)
            val s300 = Color(0xFF97DED4); val s400 = Color(0xFF73D1C4); val s500 = Color(0xFF36BAAD)
            val s600 = Color(0xFF0B9E92); val s700 = Color(0xFF0F736B); val s800 = Color(0xFF115C57)
            val s900 = Color(0xFF114541)
        }
        object Cyan {
            val s50 = Color(0xFFF5FBFC); val s100 = Color(0xFFDDF7FF); val s200 = Color(0xFFB3E8F7)
            val s300 = Color(0xFF99E2F8); val s400 = Color(0xFF72D5F3); val s500 = Color(0xFF3BBDE5)
            val s600 = Color(0xFF32A4C7); val s700 = Color(0xFF267A94); val s800 = Color(0xFF125C73)
            val s900 = Color(0xFF164759)
        }
        object Purple {
            val s50 = Color(0xFFFDFAFF); val s100 = Color(0xFFF6E9FF); val s200 = Color(0xFFECD3FF)
            val s300 = Color(0xFFE2B9FC); val s400 = Color(0xFFCFA1F2); val s500 = Color(0xFF9C45E3)
            val s600 = Color(0xFF8642C2); val s700 = Color(0xFF6E399D); val s800 = Color(0xFF5C2F83)
            val s900 = Color(0xFF401863)
        }
        object Pink {
            val s50 = Color(0xFFFFF7FC); val s100 = Color(0xFFFDE8F5); val s200 = Color(0xFFFFD5F0)
            val s300 = Color(0xFFF9B9E0); val s400 = Color(0xFFF6A7D6); val s500 = Color(0xFFE34AA6)
            val s600 = Color(0xFFCF3A96); val s700 = Color(0xFF9C2671); val s800 = Color(0xFF801458)
            val s900 = Color(0xFF570F3E)
        }
        object Violet {
            val s50 = Color(0xFFFBFAFF); val s100 = Color(0xFFF0EBFF); val s200 = Color(0xFFDBD5FF)
            val s300 = Color(0xFFC9BAFB); val s400 = Color(0xFFB3A1F5); val s500 = Color(0xFF6846E3)
            val s600 = Color(0xFF5F46C7); val s700 = Color(0xFF4F3DA1); val s800 = Color(0xFF392980)
            val s900 = Color(0xFF251959)
        }
    }

    object Dark {
        object Gray {
            val s50 = Color(0xFFF8F8F8); val s100 = Color(0xFFD4D4D4); val s200 = Color(0xFFAFAFAF)
            val s250 = Color(0xFF999999); val s300 = Color(0xFF808080); val s400 = Color(0xFF717171)
            val s500 = Color(0xFF424242); val s600 = Color(0xFF343434); val s650 = Color(0xFF2B2B2B)
            val s700 = Color(0xFF232323); val s800 = Color(0xFF1C1C1C); val s900 = Color(0xFF0F0F0F)
        }
        object Blue {
            val s50 = Color(0xFFC9E0F5); val s100 = Color(0xFFADD2F5); val s200 = Color(0xFF8CC1EC)
            val s300 = Color(0xFF5AAEF2); val s400 = Color(0xFF3294E3); val s500 = Color(0xFF1580D8)
            val s600 = Color(0xFF155999); val s700 = Color(0xFF063D71); val s800 = Color(0xFF052B53)
            val s900 = Color(0xFF0E2037)
        }
        object Green {
            val s50 = Color(0xFFC8F3DE); val s100 = Color(0xFF9BE6C1); val s200 = Color(0xFF78D7A9)
            val s300 = Color(0xFF58C08E); val s400 = Color(0xFF1BA964); val s500 = Color(0xFF0A9752)
            val s600 = Color(0xFF0F814A); val s700 = Color(0xFF035831); val s800 = Color(0xFF0A3F27)
            val s900 = Color(0xFF0B2E1C)
        }
        object Red {
            val s50 = Color(0xFFFFC1C1); val s100 = Color(0xFFFF9595); val s200 = Color(0xFFFC7474)
            val s300 = Color(0xFFEB4D52); val s400 = Color(0xFFE43838); val s500 = Color(0xFFC12020)
            val s600 = Color(0xFF901818); val s700 = Color(0xFF681916); val s800 = Color(0xFF521515)
            val s900 = Color(0xFF361515)
        }
        object Amber {
            val s50 = Color(0xFFF9E8A5); val s100 = Color(0xFFF8D16E); val s200 = Color(0xFFF0BA31)
            val s300 = Color(0xFFE79913); val s400 = Color(0xFFE37D00); val s500 = Color(0xFFCB6D10)
            val s600 = Color(0xFF824108); val s700 = Color(0xFF603007); val s800 = Color(0xFF4B2606)
            val s900 = Color(0xFF371E06)
        }
        object Orange {
            val s50 = Color(0xFFFFCDAD); val s100 = Color(0xFFFFA873); val s200 = Color(0xFFFA8A40)
            val s300 = Color(0xFFDE6D1B); val s400 = Color(0xFFC45A0E); val s500 = Color(0xFF984509)
            val s600 = Color(0xFF823906); val s700 = Color(0xFF683108); val s800 = Color(0xFF532707)
            val s900 = Color(0xFF401F07)
        }
        object Purple {
            val s50 = Color(0xFFE5C6FB); val s100 = Color(0xFFD9AFF5); val s200 = Color(0xFFC993EF)
            val s300 = Color(0xFFB168E8); val s400 = Color(0xFF984BD8); val s500 = Color(0xFF7A2DB9)
            val s600 = Color(0xFF591F89); val s700 = Color(0xFF47176E); val s800 = Color(0xFF391457)
            val s900 = Color(0xFF2E1146)
        }
        object Violet {
            val s50 = Color(0xFFDACBF7); val s100 = Color(0xFFC4AFEE); val s200 = Color(0xFFB398EF)
            val s300 = Color(0xFF9D7CEA); val s400 = Color(0xFF8867E8); val s500 = Color(0xFF5C3FC2)
            val s600 = Color(0xFF4639A6); val s700 = Color(0xFF332978); val s800 = Color(0xFF281E5D)
            val s900 = Color(0xFF221C42)
        }
    }

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
}
