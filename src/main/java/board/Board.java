package board;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.IntStream.range;

public class Board implements Constants {

    public int[] color = new int[64];
    public int[] piece = new int[64];

    public Piece[] pieces = new Piece[64];

    public int trait;
    public int notrait;

    public int castle;
    public int ep;
    public List<Move> pseudomoves = new ArrayList<>();
    public int halfMoveClock;
    public int plyNumber;
    public String[] piece_char_light = {"P", "N", "B", "R", "Q", "K"};
    public String[] piece_char_dark = {"p", "n", "b", "r", "q", "k"};
    private int fifty;
    private UndoMove um = new UndoMove();

    public Board() {
    }

    public Board(Board board) {
        color = board.color;
        piece = board.piece;
        trait = board.trait;
        notrait = board.notrait;
        castle = board.castle;
        ep = board.ep;
        fifty = board.fifty;
        pseudomoves = new ArrayList<>();
        um = new UndoMove();
    }

    private boolean in_check(int s) {
        for (int i = 0; i < 64; ++i) {
            if (piece[i] == ROI && color[i] == s) {
                return attack(i, s ^ 1);
            }
        }
        return true; // shouldn't get here
    }

    private boolean attack(int sq, int s) {
        for (int i = 0; i < 64; ++i) {
            if (color[i] == s) {
                if (piece[i] == PION) {
                    if (s == BLANC) {
                        if ((i & 7) != 0 && i - 9 == sq) {
                            return true;
                        }
                        if ((i & 7) != 7 && i - 7 == sq) {
                            return true;
                        }
                    } else {
                        if ((i & 7) != 0 && i + 7 == sq) {
                            return true;
                        }
                        if ((i & 7) != 7 && i + 9 == sq) {
                            return true;
                        }
                    }
                } else {
                    for (int j = 0; j < nbdir[piece[i]]; ++j) {
                        for (int n = i; ; ) {
                            n = g(piece[i], n, j);
                            if (n == -1) {
                                break;
                            }
                            if (n == sq) {
                                return true;
                            }
                            if (color[n] != VIDE) {
                                break;
                            }
                            if (!glisse[piece[i]]) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void gen() {
             for (int c = 0; c < 64; ++c) {
            if (color_trait(c)) {
                if (piece[c] == PION) {
                    if (trait == BLANC) {
                        if ((c & 7) != 0 && color[c - 9] == NOIR) {
                            gen_push(c, c - 9, 17);
                        }
                        if ((c & 7) != 7 && color[c - 7] == NOIR) {
                            gen_push(c, c - 7, 17);
                        }
                        if (color[c - 8] == VIDE) {
                            gen_push(c, c - 8, 16);
                            if (c >= 48 && color[c - 16] == VIDE) {
                                gen_push(c, c - 16, 24);
                            }
                        }
                    } else {
                        if ((c & 7) != 0 && color[c + 7] == BLANC) {
                            gen_push(c, c + 7, 17);
                        }
                        if ((c & 7) != 7 && color[c + 9] == BLANC) {
                            gen_push(c, c + 9, 17);
                        }
                        if (color[c + 8] == VIDE) {
                            gen_push(c, c + 8, 16);
                            if (c <= 15 && color[c + 16] == VIDE) {
                                gen_push(c, c + 16, 24);
                            }
                        }
                    }
                } else {
                    // autres pieces que pions
                    gen(c);
                    //
                }
            }
        }

        /* generate castle moves */
        if (trait == BLANC) {
            if ((castle & 1) != 0) {
                gen_push(E1, G1, 2);
            }
            if ((castle & 2) != 0) {
                gen_push(E1, C1, 2);
            }
        } else {
            if ((castle & 4) != 0) {
                gen_push(E8, G8, 2);
            }
            if ((castle & 8) != 0) {
                gen_push(E8, C8, 2);
            }
        }

        /* generate en passant moves */
        if (ep != -1) {
            if (trait == BLANC) {
                if ((ep & 7) != 0 && color[ep + 7] == BLANC && piece[ep + 7] == PION) {
                    gen_push(ep + 7, ep, 21);
                }
                if ((ep & 7) != 7 && color[ep + 9] == BLANC && piece[ep + 9] == PION) {
                    gen_push(ep + 9, ep, 21);
                }
            } else {
                if ((ep & 7) != 0 && color[ep - 9] == NOIR && piece[ep - 9] == PION) {
                    gen_push(ep - 9, ep, 21);
                }
                if ((ep & 7) != 7 && color[ep - 7] == NOIR && piece[ep - 7] == PION) {
                    gen_push(ep - 7, ep, 21);
                }
            }
        }

    }

    private boolean color_trait(int c) {
        return color[c] == trait;
    }

    private void gen(int cO) {
        int p = piece[cO];

        switch (piece[cO]) {
            case CAVALIER:
            case ROI:
                range(0, 8).forEach(d -> CAVALIER_ROI(cO, p, d, cO));
                break;
            default:
                range(0, (p == DAME ? 8 : 4))
                        .forEach(d -> DAME_FOU_TOUR(cO, p, d, cO));
                break;
        }
    }

    private void CAVALIER_ROI(int cO, int p, int d, int cX) {
        while ((cX = g(p, cX, d)) != -1) {
            if (test(cO, cX)) break;
            gen_push(cO, cX, 0);
            break;
        }
    }

    private void DAME_FOU_TOUR(int cO, int p, int d, int cX) {
        while ((cX = g(p, cX, d)) != -1) {
            if (test(cO, cX)) break;
            gen_push(cO, cX, 0);
        }
    }

    private boolean test(int cO, int cX) {
        int couleur = color[cX];
        if (couleur == VIDE) return false;
        if (couleur == notrait) gen_push(cO, cX, 1);
        return true;
    }

    private int g(int p, int cX, int d) {
        return mailbox[mailbox64[cX] + dirs[p][d]];
    }

    private void gen_push(int from, int to, int bits) {
        if ((bits & 16) != 0) {
            if (trait == BLANC) {
                if (to <= H8) {
                    gen_promote(from, to, bits);
                    return;
                }
            } else if (to >= A1) {
                gen_promote(from, to, bits);
                return;
            }
        }
        pseudomoves.add(new Move((byte) from, (byte) to, (byte) 0, (byte) bits));

    }

    private void gen_promote(int from, int to, int bits) {
        for (int i = CAVALIER; i <= DAME; ++i) {
            pseudomoves.add(new Move((byte) from, (byte) to, (byte) i, (byte) (bits | 32)));
        }
    }

    public boolean makemove(Move m) {
        if ((m.bits & 2) != 0) {
            int from;
            int to;

            if (in_check(trait)) {
                return false;
            }
            switch (m.to) {
                case 62:
                    if (color[F1] != VIDE || color[G1] != VIDE || attack(F1, notrait) || attack(G1, notrait)) {
                        return false;
                    }
                    from = H1;
                    to = F1;
                    break;
                case 58:
                    if (color[B1] != VIDE || color[C1] != VIDE || color[D1] != VIDE || attack(C1, notrait) || attack(D1, notrait)) {
                        return false;
                    }
                    from = A1;
                    to = D1;
                    break;
                case 6:
                    if (color[F8] != VIDE || color[G8] != VIDE || attack(F8, notrait) || attack(G8, notrait)) {
                        return false;
                    }
                    from = H8;
                    to = F8;
                    break;
                case 2:
                    if (color[B8] != VIDE || color[C8] != VIDE || color[D8] != VIDE || attack(C8, notrait) || attack(D8, notrait)) {
                        return false;
                    }
                    from = A8;
                    to = D8;
                    break;
                default: // shouldn't get here
                    from = -1;
                    to = -1;
                    break;
            }
            color[to] = color[from];
            piece[to] = piece[from];
            color[from] = VIDE;
            piece[from] = VIDE;
        }

        /* back up information, so we can take the move back later. */
        um.mov = m;
        um.capture = piece[m.to];
        um.castle = castle;
        um.ep = ep;
        um.fifty = fifty;

        castle &= castle_mask[m.from] & castle_mask[m.to];

        if ((m.bits & 8) != 0) {
            if (trait == BLANC) {
                ep = m.to + 8;
            } else {
                ep = m.to - 8;
            }
        } else {
            ep = -1;
        }
        if ((m.bits & 17) != 0) {
            fifty = 0;
        } else {
            ++fifty;
        }

        /* move the piece */
        color[m.to] = trait;
        if ((m.bits & 32) != 0) {
            piece[m.to] = m.promote;
        } else {
            piece[m.to] = piece[m.from];
        }
        color[m.from] = VIDE;
        piece[m.from] = VIDE;

        /* erase the pawn if this is an en passant move */
        if ((m.bits & 4) != 0) {
            if (trait == BLANC) {
                color[m.to + 8] = VIDE;
                piece[m.to + 8] = VIDE;
            } else {
                color[m.to - 8] = VIDE;
                piece[m.to - 8] = VIDE;
            }
        }

        trait ^= 1;
        notrait ^= 1;
        if (in_check(notrait)) {
            takeback();
            return false;
        }

        return true;
    }

    public void takeback() {

        trait ^= 1;
        notrait ^= 1;

        Move m = um.mov;
        castle = um.castle;
        ep = um.ep;
        fifty = um.fifty;

        color[m.from] = trait;
        if ((m.bits & 32) != 0) {
            piece[m.from] = PION;
        } else {
            piece[m.from] = piece[m.to];
        }
        if (um.capture == VIDE) {
            color[m.to] = VIDE;
            piece[m.to] = VIDE;
        } else {
            color[m.to] = notrait;
            piece[m.to] = um.capture;
        }
        if ((m.bits & 2) != 0) {
            int from;
            int to;

            switch (m.to) {
                case 62:
                    from = F1;
                    to = H1;
                    break;
                case 58:
                    from = D1;
                    to = A1;
                    break;
                case 6:
                    from = F8;
                    to = H8;
                    break;
                case 2:
                    from = D8;
                    to = A8;
                    break;
                default: // shouldn't get here
                    from = -1;
                    to = -1;
                    break;
            }
            color[to] = trait;
            piece[to] = TOUR;
            color[from] = VIDE;
            piece[from] = VIDE;
        }
        if ((m.bits & 4) != 0) {
            if (trait == BLANC) {
                color[m.to + 8] = notrait;
                piece[m.to + 8] = PION;
            } else {
                color[m.to - 8] = notrait;
                piece[m.to - 8] = PION;
            }
        }
    }

    public void print_board() {
        int i;

        System.out.print("\n8 ");
        for (i = 0; i < 64; ++i) {
            switch (color[i]) {
                case VIDE:
                    System.out.print(". ");
                    break;
                case BLANC:
                    System.out.printf(piece_char_light[piece[i]] + " ");
                    break;
                case NOIR:
                    System.out.printf(piece_char_dark[piece[i]] + " ");
                    break;
            }
            if ((i + 1) % 8 == 0 && i != 63) {
                System.out.printf("\n%d ", 7 - (i >> 3));
            }
        }
        System.out.print("\n\n   a b c d e f g h\n\n");
    }
}
